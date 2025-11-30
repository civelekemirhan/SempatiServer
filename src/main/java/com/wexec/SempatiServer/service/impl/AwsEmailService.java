package com.wexec.SempatiServer.service.impl;

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

    // Şu an burası çalışmakta lakin AWS SES kullanmıyoruz
    // çünkü AWS SES sadece AWS tarafından doğrulanmış maillere mail göndermeye izin
    // veriyor bu da bizi kısıtlıyordu
    // AWS SES kullanılacaksa veya bununla ilgili bir çözüm sunulacaksa bana ulaşın
    // property ayarlarını size vereyim

    private final JavaMailSender mailSender;

    @Async("taskExecutor") // Bu metod ayrı bir thread'de çalışır, kullanıcıyı bekletmez!
    @Override
    public void sendVerificationCode(String to, String code) {
        log.info("Mail gönderimi başlatılıyor: {}", to);
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("sempati.app@gmail.com"); // AWS SES'te doğruladığın mail
            message.setTo(to);
            message.setSubject("Sempati Doğrulama Kodu");
            message.setText("Hoş geldin! Doğrulama kodun: " + code);

            mailSender.send(message);
            log.info("Mail başarıyla gönderildi: {}", to);
        } catch (Exception e) {
            log.error("Mail gönderilirken hata oluştu: {}", e.getMessage());
            // Burada retry mekanizması düşünülebilir ama şimdilik log yeterli.
        }
    }

    @Override
    @Async("taskExecutor")
    public void sendPasswordResetCode(String to, String code) {
        log.info("Şifre sıfırlama kodu gönderiliyor: {}", to);

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("sempati.app@gmail.com");
            message.setTo(to);
            message.setSubject("Sempati Şifre Sıfırlama Kodu");
            message.setText("Şifre sıfırlama kodunuz: " + code + "\nBu kod 5 dakika boyunca geçerlidir.");

            mailSender.send(message);
            log.info("Şifre sıfırlama maili gönderildi: {}", to);

        } catch (Exception e) {
            log.error("Şifre sıfırlama maili gönderilemedi: {}", e.getMessage());
        }
    }

}
