package com.wexec.SempatiServer.service;

import com.wexec.SempatiServer.common.BusinessException;
import com.wexec.SempatiServer.common.ErrorCode;
import com.wexec.SempatiServer.common.GenericResponse;
import com.wexec.SempatiServer.dto.ChatMessageRequest;
import com.wexec.SempatiServer.entity.ChatMessage;
import com.wexec.SempatiServer.entity.MessageType;
import com.wexec.SempatiServer.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final S3Service s3Service;

    @Transactional // Veritabanı işlemi olduğu için transactional ekledik
    public void saveAndSendMessage(Long senderId, ChatMessageRequest request) {

        // Yeni Hata Yönetimi: Boş mesaj kontrolü
        if (request.getContent() == null || request.getContent().trim().isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Mesaj içeriği boş olamaz.");
        }

        String chatId = getChatId(senderId, request.getRecipientId());

        ChatMessage message = ChatMessage.builder()
                .chatId(chatId)
                .senderId(senderId)
                .recipientId(request.getRecipientId())
                .content(request.getContent())
                .type(request.getType())
                .timestamp(LocalDateTime.now())
                .build();

        chatMessageRepository.save(message);

        // WebSocket üzerinden alıcıya gönder
        messagingTemplate.convertAndSendToUser(
                String.valueOf(request.getRecipientId()),
                "/queue/messages",
                message);
    }

    @Transactional
    public GenericResponse<ChatMessage> uploadAndSendMedia(Long senderId, Long recipientId, MultipartFile file,
            MessageType type) {

        // Yeni Hata Yönetimi: Boş dosya kontrolü
        if (file.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Gönderilecek dosya boş olamaz.");
        }

        // 1. Dosyayı AWS S3'e yükle (Hata olursa S3Service kendi içinde fırlatır,
        // GlobalHandler yakalar)
        String mediaUrl = s3Service.uploadFile(file);

        // 2. Normal bir mesajmış gibi hazırla
        ChatMessageRequest request = new ChatMessageRequest();
        request.setRecipientId(recipientId);
        request.setContent(mediaUrl);
        request.setType(type);

        // 3. Mesajı kaydet ve gönder
        saveAndSendMessage(senderId, request);

        // 4. Response dön (chatId ekledim ki tutarlı olsun)
        ChatMessage sentMessage = ChatMessage.builder()
                .chatId(getChatId(senderId, recipientId))
                .senderId(senderId)
                .recipientId(recipientId)
                .content(mediaUrl)
                .type(type)
                .timestamp(LocalDateTime.now())
                .build();

        return GenericResponse.success(sentMessage);
    }

    // --- GET / HELPERS ---

    public GenericResponse<List<ChatMessage>> getChatHistory(Long userId1, Long userId2) {
        String chatId = getChatId(userId1, userId2);
        List<ChatMessage> history = chatMessageRepository.findByChatIdOrderByTimestampAsc(chatId);
        return GenericResponse.success(history);
    }

    private String getChatId(Long senderId, Long recipientId) {
        if (senderId < recipientId)
            return senderId + "_" + recipientId;
        else
            return recipientId + "_" + senderId;
    }
}