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

    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;

    @Transactional
    public GenericResponse<String> register(RegisterRequest request) {
        // 1. Şifre Kontrolü
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException("Şifreler eşleşmiyor.", "AUTH_PASS_MISMATCH");
        }

        // 2. Email Kontrolü (Akıllı Kontrol)
        Optional<User> existingUserOpt = userRepository.findByEmail(request.getEmail());

        if (existingUserOpt.isPresent()) {
            User existingUser = existingUserOpt.get();

            // A) Eğer hesap zaten doğrulanmışsa -> HATA FIRLAT
            if (existingUser.isEnabled()) {
                throw new BusinessException("Bu e-posta adresi zaten kullanımda.", "AUTH_EMAIL_EXISTS");
            }

            // B) Hesap var ama doğrulanmamış -> GÜNCELLE VE TEKRAR KOD AT
            else {
                // Yeni kod üret
                String newCode = String.valueOf(new Random().nextInt(900000) + 100000);

                // Kullanıcı bilgilerini güncelle (Belki şifresini yanlış yazmıştı, düzeltti)
                existingUser.setNickname(request.getNickname());
                existingUser.setPhoneNumber(request.getPhoneNumber());
                existingUser.setPassword(passwordEncoder.encode(request.getPassword()));
                userRepository.save(existingUser);

                // Token tablosunu güncelle
                VerificationToken token = verificationTokenRepository.findByUser(existingUser)
                        .orElse(VerificationToken.builder().user(existingUser).build());

                token.setToken(newCode);
                token.setExpiresAt(LocalDateTime.now().plusMinutes(2));
                verificationTokenRepository.save(token);

                // Mail gönder
                emailService.sendVerificationCode(existingUser.getEmail(), newCode);

                return GenericResponse.success("Önceki kayıt doğrulanmamıştı. Yeni doğrulama kodu gönderildi.");
            }
        }

        // 3. HİÇ KAYDI YOKSA -> SIFIRDAN OLUŞTUR (Eski Mantık)
        User user = User.builder()
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .nickname(request.getNickname())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .enabled(false)
                .build();

        User savedUser = userRepository.save(user);

        // Token oluştur
        String code = String.valueOf(new Random().nextInt(900000) + 100000);
        VerificationToken verificationToken = VerificationToken.builder()
                .token(code)
                .user(savedUser)
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .build();
        verificationTokenRepository.save(verificationToken);

        // Mail gönder
        emailService.sendVerificationCode(user.getEmail(), code);

        return GenericResponse.success("Kayıt başarılı. Doğrulama kodu gönderildi.");
    }

    @Transactional
    public GenericResponse<AuthResponse> verify(String email, String code) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı"));

        // Verification tablosundan kodu bul
        VerificationToken verificationToken = verificationTokenRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Doğrulama kodu bulunamadı"));

        if (verificationToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            return GenericResponse.error(400, "Kodun süresi dolmuş.", "AUTH_003");
        }

        if (!verificationToken.getToken().equals(code)) {
            return GenericResponse.error(400, "Hatalı kod.", "AUTH_004");
        }

        // Başarılı -> Kullanıcıyı aktifleştir
        user.setEnabled(true);
        userRepository.save(user);

        // Kullanılan token'ı silebiliriz (Temizlik)
        verificationTokenRepository.delete(verificationToken);

        // Giriş yapılmış gibi token dön
        return generateTokensAndSave(user);
    }

    public GenericResponse<AuthResponse> login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (Exception e) {
            return GenericResponse.error(401, "Hatalı giriş bilgileri.", "AUTH_005");
        }

        User user = userRepository.findByEmail(request.getEmail()).orElseThrow();
        return generateTokensAndSave(user);
    }

    // --- Helper Method: Token Üret ve DB'ye Kaydet ---
    private GenericResponse<AuthResponse> generateTokensAndSave(User user) {
        String accessToken = jwtService.generateAccessToken(user);

        // Refresh Token'ı biz üretiyoruz (UUID veya JWT olabilir, UUID yeterlidir ve güvenlidir)
        String refreshTokenStr = UUID.randomUUID().toString();

        RefreshToken refreshToken = RefreshToken.builder()
                .token(refreshTokenStr)
                .user(user)
                .expiryDate(Instant.now().plusMillis(1000L * 60 * 60 * 24 * 30)) // 30 Gün
                .revoked(false)
                .build();

        refreshTokenRepository.save(refreshToken); // DB'ye Kayıt!

        return GenericResponse.success(new AuthResponse(accessToken, refreshTokenStr));
    }
}