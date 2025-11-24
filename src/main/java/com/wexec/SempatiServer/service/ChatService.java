package com.wexec.SempatiServer.service;

import com.wexec.SempatiServer.common.GenericResponse;
import com.wexec.SempatiServer.dto.ChatMessageRequest;
import com.wexec.SempatiServer.entity.ChatMessage;
import com.wexec.SempatiServer.entity.MessageType;
import com.wexec.SempatiServer.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final S3Service s3Service; // AWS S3 Servisin

    // ... saveAndSendMessage metodu aynen kalıyor, sadece type set etmeyi ekle ...
    public void saveAndSendMessage(Long senderId, ChatMessageRequest request) {
        String chatId = getChatId(senderId, request.getRecipientId());

        ChatMessage message = ChatMessage.builder()
                .chatId(chatId)
                .senderId(senderId)
                .recipientId(request.getRecipientId())
                .content(request.getContent())
                .type(request.getType()) // Tipi set et
                .timestamp(LocalDateTime.now())
                .build();

        chatMessageRepository.save(message);

        messagingTemplate.convertAndSendToUser(
                String.valueOf(request.getRecipientId()),
                "/queue/messages",
                message
        );
    }

    // --- YENİ: Medya (Resim/Ses) Yükleyip Gönderme ---
    public GenericResponse<ChatMessage> uploadAndSendMedia(Long senderId, Long recipientId, MultipartFile file, MessageType type) {
        // 1. Dosyayı AWS S3'e yükle
        String mediaUrl = s3Service.uploadFile(file);

        // 2. Normal bir mesajmış gibi hazırla (İçerik = URL)
        ChatMessageRequest request = new ChatMessageRequest();
        request.setRecipientId(recipientId);
        request.setContent(mediaUrl);
        request.setType(type);

        // 3. Mesajı kaydet ve WebSocket ile gönder
        saveAndSendMessage(senderId, request);

        // 4. Response olarak dön (Gönderen kişi de ekranda görsün diye)
        // Not: saveAndSendMessage void olduğu için burada objeyi tekrar oluşturuyoruz veya save metodunu return eder hale getirebilirsin.
        // Pratik çözüm:
        ChatMessage sentMessage = ChatMessage.builder()
                .senderId(senderId)
                .recipientId(recipientId)
                .content(mediaUrl)
                .type(type)
                .timestamp(LocalDateTime.now())
                .build();

        return GenericResponse.success(sentMessage);
    }

    // ... getChatId ve getChatHistory metodları aynı ...
    private String getChatId(Long senderId, Long recipientId) {
        if (senderId < recipientId) return senderId + "_" + recipientId;
        else return recipientId + "_" + senderId;
    }
    public GenericResponse<List<ChatMessage>> getChatHistory(Long userId1, Long userId2) {
        String chatId = getChatId(userId1, userId2);
        List<ChatMessage> history = chatMessageRepository.findByChatIdOrderByTimestampAsc(chatId);
        return GenericResponse.success(history);
    }
}