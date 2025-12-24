package com.wexec.SempatiServer.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.wexec.SempatiServer.common.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class FcmService {

    /**
     * Belirtilen FCM token'a (Android cihaza) bildirim gönderir.
     */
    public void sendNotification(String token, String title, String body, String senderId) {
        if (token == null || token.isEmpty()) {
            log.warn("Kullanıcının FCM Token'ı yok, bildirim gönderilemedi.");
            return;
        }

        try {
            // 1. Bildirim Görünümü
            Notification notification = Notification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .build();

            // 2. Mesaj Verisi (Data Payload)
            Message message = Message.builder()
                    .setToken(token)
                    .setNotification(notification)
                    .putData("senderId", senderId)
                    .putData("click_action", "OPEN_CHAT_ACTIVITY")
                    .build();

            // 3. Gönder
            String response = FirebaseMessaging.getInstance().send(message);
            log.info("FCM Bildirimi başarıyla gönderildi: " + response);

        } catch (Exception e) {
            // Hata olsa bile işlemi durdurma, sadece logla
            log.error("FCM Bildirim Hatası [{}]: {}", ErrorCode.INTERNAL_SERVER_ERROR.getCode(), e.getMessage());
        }
    }
}