package com.wexec.SempatiServer.service;

import com.wexec.SempatiServer.common.BusinessException;
import com.wexec.SempatiServer.common.ErrorCode; // <--- YENİ IMPORT
import com.wexec.SempatiServer.dto.ForgotPasswordVerifyResponse;
import com.wexec.SempatiServer.entity.User;
import com.wexec.SempatiServer.entity.VerificationCode;
import com.wexec.SempatiServer.repository.UserRepository;
import com.wexec.SempatiServer.repository.VerificationCodeRepository;
import com.wexec.SempatiServer.dto.ForgotPasswordRequest;
import com.wexec.SempatiServer.dto.VerifyCodeRequest;
import com.wexec.SempatiServer.dto.ResetPasswordRequest;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final UserRepository userRepository;
    private final VerificationCodeRepository codeRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    // 1. ADIM: Mail Adresine Kod Gönder
    @Transactional
    public void initiatePasswordReset(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        String code = String.format("%06d", new Random().nextInt(999999));

        VerificationCode verificationCode = codeRepository.findByUser(user)
                .orElse(new VerificationCode());

        verificationCode.setUser(user);
        verificationCode.setCode(code);
        verificationCode.setExpiryDate(LocalDateTime.now().plusMinutes(2));
        verificationCode.setUsed(false);
        verificationCode.setResetToken(null);

        codeRepository.save(verificationCode);

        emailService.sendPasswordResetCode(user.getEmail(), code);
    }

    // 2. ADIM: Kodu Doğrula ve Reset Token Üret
    @Transactional
    public ForgotPasswordVerifyResponse verifyCode(VerifyCodeRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        VerificationCode vCode = codeRepository.findByUserAndUsedFalse(user)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESET_CODE_NOT_FOUND));

        if (vCode.isExpired()) {
            throw new BusinessException(ErrorCode.RESET_CODE_EXPIRED);
        }

        if (!vCode.getCode().equals(request.getCode())) {
            throw new BusinessException(ErrorCode.RESET_INVALID_CODE);
        }

        String resetToken = UUID.randomUUID().toString();
        vCode.setResetToken(resetToken);
        vCode.setUsed(true);
        codeRepository.save(vCode);

        return ForgotPasswordVerifyResponse.builder()
                .resetToken(resetToken)
                .build();
    }

    // 3. ADIM: Şifreyi Güncelle
    @Transactional
    public void updatePassword(ResetPasswordRequest request) {
        VerificationCode vCode = codeRepository.findByResetToken(request.getResetToken())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESET_INVALID_TOKEN));

        if (vCode.isExpired()) {
            throw new BusinessException(ErrorCode.RESET_TOKEN_EXPIRED);
        }

        User user = vCode.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        vCode.setResetToken(null);
        codeRepository.save(vCode);
    }
}