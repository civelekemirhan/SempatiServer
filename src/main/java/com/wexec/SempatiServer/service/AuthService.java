package com.wexec.SempatiServer.service;

import com.wexec.SempatiServer.common.BusinessException;
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
public class AuthService {

    private final UserRepository userRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    // private final PasswordResetTokenRepository passwordResetTokenRepository; //
    // SİLDİK: Artık kullanılmıyor

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
            throw new BusinessException("Şifreler eşleşmiyor.", "AUTH_PASS_MISMATCH");
        }

        Optional<User> existingUserOpt = userRepository.findByEmail(request.getEmail());

        if (existingUserOpt.isPresent()) {
            User existingUser = existingUserOpt.get();

            if (existingUser.isEnabled()) {
                throw new BusinessException("Bu e-posta adresi zaten kullanımda.", "AUTH_EMAIL_EXISTS");
            }

            // Kayıt olmuş ama doğrulamamış kullanıcı için yeni kod
            String newCode = String.valueOf(new Random().nextInt(900000) + 100000);

                // Kullanıcı bilgilerini güncelle (Belki şifresini yanlış yazmıştı, düzeltti)
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

        // Yeni Kullanıcı
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
    // 2) EMAIL VERIFY (Kayıt Olma Doğrulaması)
    // -------------------------------------------------------------
    @Transactional
    public GenericResponse<AuthResponse> verify(String email, String code) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı"));

        VerificationToken verificationToken = verificationTokenRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Doğrulama kodu bulunamadı"));

        if (verificationToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            return GenericResponse.error(400, "Kodun süresi dolmuş.", "AUTH_003");
        }

        if (!verificationToken.getToken().equals(code)) {
            return GenericResponse.error(400, "Hatalı kod.", "AUTH_004");
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
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        } catch (Exception e) {
            return GenericResponse.error(401, "Hatalı giriş bilgileri.", "AUTH_005");
        }

        User user = userRepository.findByEmail(request.getEmail()).orElseThrow();
        return generateTokensAndSave(user);
    }

    // -------------------------------------------------------------
    // NOT: Şifre Sıfırlama metodları buradan SİLİNDİ.
    // O işlemler artık "PasswordResetService" içinde yapılıyor.
    // -------------------------------------------------------------

    // -------------------------------------------------------------
    // 4) REFRESH TOKEN (BURASI EKSİKTİ, EKLENDİ)
    // -------------------------------------------------------------
    public GenericResponse<AuthResponse> refreshToken(RefreshTokenRequest request) {
        // 1. Token DB'de var mı?
        RefreshToken tokenNode = refreshTokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new BusinessException("Refresh Token bulunamadı. Lütfen tekrar giriş yapın.",
                        "REFRESH_NOT_FOUND"));

        // 2. Süresi dolmuş mu?
        if (tokenNode.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(tokenNode); // Süresi dolanı temizle
            throw new BusinessException("Oturum süresi dolmuş. Lütfen tekrar giriş yapın.", "REFRESH_EXPIRED");
        }

        // 3. Kullanıcıya yeni bir Access Token üret
        User user = tokenNode.getUser();

        // Bu işlem, bu kullanıcıya ait önceki tüm Access Token'ları (farklı cihazlarda
        // olsa bile) geçersiz kılar.
        user.setTokenVersion(user.getTokenVersion() + 1);
        userRepository.save(user);
        String newAccessToken = jwtService.generateAccessToken(user);

        // 4. Yeni Access Token'ı ve ESKİ (hala geçerli olan) Refresh Token'ı dön.
        return GenericResponse.success(new AuthResponse(newAccessToken, request.getToken()));
    }

    // -------------------------------------------------------------
    // HELPER
    // -------------------------------------------------------------
    private GenericResponse<AuthResponse> generateTokensAndSave(User user) {

        String accessToken = jwtService.generateAccessToken(user);

        String refreshTokenStr = UUID.randomUUID().toString();

        RefreshToken refreshToken = RefreshToken.builder()
                .token(refreshTokenStr)
                .user(user)
                .expiryDate(Instant.now().plusMillis(1000L * 60 * 60 * 24 * 30))
                .revoked(false)
                .build();

        refreshTokenRepository.save(refreshToken);

        return GenericResponse.success(new AuthResponse(accessToken, refreshTokenStr));
    }

}