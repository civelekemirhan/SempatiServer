package com.wexec.SempatiServer.service;

import com.wexec.SempatiServer.common.BusinessException;
import com.wexec.SempatiServer.common.ErrorCode;
import com.wexec.SempatiServer.common.GenericResponse;
import com.wexec.SempatiServer.dto.*;
import com.wexec.SempatiServer.entity.ChatMessage;
import com.wexec.SempatiServer.entity.MessageType;
import com.wexec.SempatiServer.entity.User;
import com.wexec.SempatiServer.repository.ChatMessageRepository;
import com.wexec.SempatiServer.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@SuppressWarnings("null") // IDE'nin JPA/Lombok kaynaklı gereksiz null uyarılarını susturur
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final S3Service s3Service;
    private final FcmService fcmService;
    private final UserRepository userRepository;

    // 1. MESAJ GÖNDERME (TEXT)
    // 1. MESAJ GÖNDERME (TEXT)
    // ChatService.java içinde bu metodu güncelle:

    @Transactional
    public ChatMessage saveAndSendMessage(Long senderId, ChatMessageRequest request) {
        // 1. Validasyon
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
                .isRead(false)
                .build();

        // 2. Veritabanına Kaydet
        ChatMessage savedMessage = chatMessageRepository.save(message);

        // 3. WebSocket ile Canlı Gönder (GÜNCELLENDİ)

        // Gönderen kişinin ismini ve resmini bulmamız lazım
        User senderUser = userRepository.findById(senderId).orElse(null);

        // Alıcıyı buluyoruz (Email adresine yollamak için)
        User recipientUser = userRepository.findById(request.getRecipientId()).orElse(null);

        if (recipientUser != null && senderUser != null) {

            // DTO HAZIRLIĞI: Mesaj verisi + Gönderen Kimliği
            SocketMessageDto socketPayload = SocketMessageDto.builder()
                    .messageId(savedMessage.getId())
                    .content(savedMessage.getContent())
                    .type(savedMessage.getType())
                    .timestamp(savedMessage.getTimestamp())
                    // UI için kritik veriler:
                    .senderId(senderUser.getId())
                    .senderName(senderUser.getNickname())     // <-- İsim eklendi
                    .senderIcon(senderUser.getProfileIcon())  // <-- Resim eklendi
                    .build();

            // WebSocket ile DTO'yu Gönder (Artık Entity gitmiyor, DTO gidiyor)
            messagingTemplate.convertAndSendToUser(
                    recipientUser.getEmail(),
                    "/queue/messages",
                    socketPayload);

            System.out.println("✅ Mesaj DTO olarak yollandı: " + recipientUser.getEmail());

            // 4. FCM Bildirimi (Değişmedi)
            sendPushNotification(senderId, request);
        }

        return savedMessage;
    }

    // Yardımcı: Bildirim Gönderimi
    private void sendPushNotification(Long senderId, ChatMessageRequest request) {
        User sender = userRepository.findById(senderId).orElse(null);
        User recipient = userRepository.findById(request.getRecipientId()).orElse(null);

        if (sender != null && recipient != null && recipient.getFcmToken() != null) {
            String title = sender.getNickname();
            String body = request.getType() == MessageType.IMAGE ? "📷 Bir fotoğraf gönderdi" : request.getContent();

            fcmService.sendNotification(recipient.getFcmToken(), title, body, String.valueOf(senderId));
        }
    }

    // 2. MEDYA GÖNDERME (IMAGE/AUDIO)
    @Transactional
    public GenericResponse<ChatMessage> uploadAndSendMedia(Long senderId, Long recipientId, MultipartFile file,
            MessageType type) {

        if (file.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Gönderilecek dosya boş olamaz.");
        }

        // 1. Dosyayı S3'e yükle
        String mediaUrl = s3Service.uploadFile(file);

        // 2. Mesaj isteği hazırla (İçerik = URL)
        ChatMessageRequest request = new ChatMessageRequest();
        request.setRecipientId(recipientId);
        request.setContent(mediaUrl);
        request.setType(type);

        // 3. Kaydet, Socket'ten yolla, Bildirim at
        ChatMessage sentMessage = saveAndSendMessage(senderId, request);

        return GenericResponse.success(sentMessage);
    }

    // 3. LİSTELEME İŞLEMLERİ

    // Son Sohbetler Listesi (WhatsApp Ana Ekranı)
    public GenericResponse<List<ChatSummaryDto>> getRecentChats(Long currentUserId) {
        List<ChatMessage> lastMessages = chatMessageRepository.findRecentChats(currentUserId);
        List<ChatSummaryDto> summaries = new ArrayList<>();

        for (ChatMessage msg : lastMessages) {
            Long otherUserId = msg.getSenderId().equals(currentUserId) ? msg.getRecipientId() : msg.getSenderId();
            User otherUser = userRepository.findById(otherUserId).orElse(null);

            if (otherUser != null) {
                summaries.add(ChatSummaryDto.builder()
                        .userId(otherUser.getId())
                        .nickname(otherUser.getNickname())
                        .profileIcon(otherUser.getProfileIcon())
                        .lastMessage(msg.getType() == MessageType.IMAGE ? "📷 Fotoğraf" : msg.getContent())
                        .type(msg.getType())
                        .unreadCount(chatMessageRepository.countByRecipientIdAndIsReadFalse(otherUserId))
                        .timestamp(msg.getTimestamp())
                        .build());
            }
        }
        return GenericResponse.success(summaries);
    }

    public GenericResponse<PagedResponse<ChatMessage>> getChatHistory(Long userId1, Long userId2, int page, int size) {
        String chatId = getChatId(userId1, userId2);

        // En yeni mesajlar önce (Desc)
        Pageable pageable = PageRequest.of(page, size);

        Page<ChatMessage> historyPage = chatMessageRepository.findByChatIdOrderByTimestampDesc(chatId, pageable);

        // Yardımcı metod ile dönüştür
        return GenericResponse.success(mapToPagedResponse(historyPage));
    }

    // 4. ETKİLEŞİM İŞLEMLERİ

    // Mesajları Okundu İşaretle
    @Transactional
    public void markMessagesAsRead(Long currentUserId, Long otherUserId) {
        // "Diğer kişiden bana gelen ve okunmamış olanları güncelle"
        chatMessageRepository.markMessagesAsRead(otherUserId, currentUserId);
    }

    // Toplam Okunmamış Mesaj Sayısı (Badge için)
    public Long getUnreadMessageCount(Long currentUserId) {
        return chatMessageRepository.countByRecipientIdAndIsReadFalse(currentUserId);
    }

    // Mesaj Silme
    @Transactional
    public void deleteMessage(Long currentUserId, Long messageId) {
        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REQUEST, "Mesaj bulunamadı."));

        // Güvenlik: Sadece kendi mesajını silebilirsin
        if (!message.getSenderId().equals(currentUserId)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Sadece kendi mesajlarınızı silebilirsiniz.");
        }

        chatMessageRepository.delete(message);
    }

    // "Yazıyor..." Bildirimi (Veritabanına yazmaz, direkt iletir)
    public void sendTypingNotification(Long senderId, TypingRequest request) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("senderId", senderId);
        payload.put("isTyping", request.isTyping());

        // Kanal: /user/{recipientId}/queue/typing
        messagingTemplate.convertAndSendToUser(
                String.valueOf(request.getRecipientId()),
                "/queue/typing",
                payload);
    }

    // --- HELPER ---
    private String getChatId(Long senderId, Long recipientId) {
        if (senderId < recipientId)
            return senderId + "_" + recipientId;
        else
            return recipientId + "_" + senderId;
    }

    private <T> PagedResponse<T> mapToPagedResponse(Page<T> page) {
        return PagedResponse.<T>builder()
                .content(page.getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }
}