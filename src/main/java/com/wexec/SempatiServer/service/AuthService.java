package com.wexec.SempatiServer.service;

import com.wexec.SempatiServer.common.BusinessException;
import com.wexec.SempatiServer.common.ErrorCode; // <--- ENUM IMPORTU ÖNEMLİ
import com.wexec.SempatiServer.common.GenericResponse;
import com.wexec.SempatiServer.dto.*;
import com.wexec.SempatiServer.entity.*;
import com.wexec.SempatiServer.repository.*;
import com.wexec.SempatiServer.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@SuppressWarnings("null") // <--- IDE'nin gereksiz "Null safety" uyarılarını susturur
public class AuthService {

    private final UserRepository userRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;

    // -------------------------------------------------------------
    // 1) REGISTER
    // -------------------------------------------------------------
    @Transactional
    public GenericResponse<String> register(RegisterRequest request) {

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException(ErrorCode.AUTH_PASSWORD_MISMATCH);
        }

        Optional<User> existingUserOpt = userRepository.findByEmail(request.getEmail());

        if (existingUserOpt.isPresent()) {
            User existingUser = existingUserOpt.get();

            if (existingUser.isEnabled()) {
                throw new BusinessException(ErrorCode.AUTH_EMAIL_ALREADY_EXISTS);
            }

            // Kayıt olmuş ama doğrulamamış kullanıcı için yeni kod
            String newCode = String.valueOf(new Random().nextInt(900000) + 100000);

            // Kullanıcı bilgilerini güncelle (PhoneNumber YOK)
            existingUser.setNickname(request.getNickname());
            existingUser.setGender(request.getGender());
            existingUser.setPassword(passwordEncoder.encode(request.getPassword()));
            userRepository.save(existingUser);

            VerificationToken token = verificationTokenRepository.findByUser(existingUser)
                    .orElse(VerificationToken.builder().user(existingUser).build());

            token.setToken(newCode);
            token.setExpiresAt(LocalDateTime.now().plusMinutes(2));
            verificationTokenRepository.save(token);

            emailService.sendVerificationCode(existingUser.getEmail(), newCode);

            return GenericResponse.success("Önceki kayıt doğrulanmamıştı. Yeni doğrulama kodu gönderildi.");
        }

        // Yeni Kullanıcı (PhoneNumber YOK)
        User user = User.builder()
                .email(request.getEmail())
                .gender(request.getGender())
                .nickname(request.getNickname())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .enabled(false)
                .build();

        User savedUser = userRepository.save(user);

        String code = String.valueOf(new Random().nextInt(900000) + 100000);
        VerificationToken verificationToken = VerificationToken.builder()
                .token(code)
                .user(savedUser)
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .build();
        verificationTokenRepository.save(verificationToken);

        emailService.sendVerificationCode(user.getEmail(), code);

        return GenericResponse.success("Kayıt başarılı. Doğrulama kodu gönderildi.");
    }

    // -------------------------------------------------------------
    // 2) EMAIL VERIFY
    // -------------------------------------------------------------
    @Transactional
    public GenericResponse<AuthResponse> verify(String email, String code) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        VerificationToken verificationToken = verificationTokenRepository.findByUser(user)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_INVALID_TOKEN));

        if (verificationToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.AUTH_TOKEN_EXPIRED);
        }

        if (!verificationToken.getToken().equals(code)) {
            throw new BusinessException(ErrorCode.AUTH_INVALID_TOKEN);
        }

        user.setEnabled(true);
        userRepository.save(user);

        verificationTokenRepository.delete(verificationToken);

        return generateTokensAndSave(user);
    }

    // -------------------------------------------------------------
    // 3) LOGIN
    // -------------------------------------------------------------
    public GenericResponse<AuthResponse> login(LoginRequest request) {
        // Hata kontrolünü Spring Security'ye bıraktık (GlobalHandler yakalayacak)
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        return generateTokensAndSave(user);
    }

    // -------------------------------------------------------------
    // 4) REFRESH TOKEN
    // -------------------------------------------------------------
    public GenericResponse<AuthResponse> refreshToken(RefreshTokenRequest request) {
        // 1. Token DB'de var mı?
        RefreshToken tokenNode = refreshTokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new BusinessException(ErrorCode.REFRESH_NOT_FOUND));

        // 2. Süresi dolmuş mu?
        if (tokenNode.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(tokenNode); // Temizlik
            throw new BusinessException(ErrorCode.REFRESH_EXPIRED);
        }

        // 3. Kullanıcıya yeni bir Access Token üret
        User user = tokenNode.getUser();

        // Güvenlik: Token versiyonunu artır
        user.setTokenVersion(user.getTokenVersion() + 1);
        userRepository.save(user);

        String newAccessToken = jwtService.generateAccessToken(user);

        return GenericResponse.success(new AuthResponse(newAccessToken, request.getToken()));
    }

    // -------------------------------------------------------------
    // HELPER
    // -------------------------------------------------------------
    private GenericResponse<AuthResponse> generateTokensAndSave(User user) {

        String accessToken = jwtService.generateAccessToken(user);

        String refreshTokenStr = UUID.randomUUID().toString();

        Optional<RefreshToken> oldToken = refreshTokenRepository.findByUser(user);
        oldToken.ifPresent(refreshTokenRepository::delete);

        RefreshToken refreshToken = RefreshToken.builder()
                .token(refreshTokenStr)
                .user(user)
                .expiryDate(Instant.now().plusMillis(1000L * 60 * 60 * 24 * 30)) // 30 Gün
                .revoked(false)
                .build();

        refreshTokenRepository.save(refreshToken);

        return GenericResponse.success(new AuthResponse(accessToken, refreshTokenStr));
    }
}