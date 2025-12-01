package com.wexec.SempatiServer.service.impl;

import com.wexec.SempatiServer.common.ErrorCode; // <--- ENUM EKLENDİ
import com.wexec.SempatiServer.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AwsEmailService implements EmailService {
    private final JavaMailSender mailSender;

    /**
     * ÖNEMLİ NOT:
     * 
     * @Async metodlar ayrı bir thread'de çalıştığı için burada fırlatılan
     *        BusinessException'lar GlobalExceptionHandler tarafından yakalanıp
     *        kullanıcıya JSON dönemez.
     *        Çünkü kullanıcıya cevap çoktan gitmiştir.
     *        Bu yüzden burada try-catch ile hatayı yakalayıp LOGLAMAK en
     *        doğrusudur.
     */

    @Async("taskExecutor")
    @Override
    public void sendVerificationCode(String to, String code) {
        log.info("Doğrulama maili hazırlanıyor: {}", to);
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("sempati.app@gmail.com");
            message.setTo(to);
            message.setSubject("Sempati Doğrulama Kodu");
            message.setText("Hoş geldin! Doğrulama kodun: " + code);

            mailSender.send(message);
            log.info("Doğrulama maili gönderildi: {}", to);

        } catch (Exception e) {
            // Hatayı fırlatmıyoruz ama standart log formatımızla kaydediyoruz
            log.error("Mail Hatası [{}]: {} - Detay: {}",
                    ErrorCode.EMAIL_SEND_ERROR.getCode(),
                    ErrorCode.EMAIL_SEND_ERROR.getMessage(),
                    e.getMessage());
        }
    }

    @Override
    @Async("taskExecutor")
    public void sendPasswordResetCode(String to, String code) {
        log.info("Şifre sıfırlama kodu hazırlanıyor: {}", to);

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("sempati.app@gmail.com");
            message.setTo(to);
            message.setSubject("Sempati Şifre Sıfırlama Kodu");
            message.setText("Şifre sıfırlama kodunuz: " + code + "\nBu kod 2 dakika boyunca geçerlidir.");

            mailSender.send(message);
            log.info("Şifre sıfırlama maili gönderildi: {}", to);

        } catch (Exception e) {
            log.error("Mail Hatası [{}]: {} - Detay: {}",
                    ErrorCode.EMAIL_SEND_ERROR.getCode(),
                    ErrorCode.EMAIL_SEND_ERROR.getMessage(),
                    e.getMessage());
        }
    }
}