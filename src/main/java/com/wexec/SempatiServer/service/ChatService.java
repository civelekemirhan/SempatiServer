package com.wexec.SempatiServer.service;

import com.wexec.SempatiServer.common.BusinessException;
import com.wexec.SempatiServer.common.ErrorCode;
import com.wexec.SempatiServer.common.GenericResponse;
import com.wexec.SempatiServer.dto.*;
import com.wexec.SempatiServer.entity.ChatMessage;
import com.wexec.SempatiServer.entity.MessageType;
import com.wexec.SempatiServer.entity.User;
import com.wexec.SempatiServer.repository.ChatMessageRepository;
import com.wexec.SempatiServer.repository.UserBlockRepository;
import com.wexec.SempatiServer.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
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
@Slf4j
@SuppressWarnings("null")
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final S3Service s3Service;
    private final FcmService fcmService;
    private final UserRepository userRepository;
    private final UserBlockRepository userBlockRepository;

    // Bu metot WebSocket Controller tarafından çağrılır (Email ile)
    @Transactional
    public void saveAndSendMessage(ChatMessageRequest request, String senderEmail) {
        User sender = userRepository.findByEmail(senderEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // Ana işlemi çağır
        processAndSendMessage(sender.getId(), request);
    }

    // Bu metot direkt ID ile çağrılır (Medya yükleme veya REST API)
    @Transactional
    public ChatMessage saveAndSendMessage(Long senderId, ChatMessageRequest request) {
        return processAndSendMessage(senderId, request);
    }

    // --- ÇEKİRDEK MANTIK (Tüm kontroller burada) ---
    private ChatMessage processAndSendMessage(Long senderId, ChatMessageRequest request) {

        // 1. Validasyon
        if (request.getContent() == null || request.getContent().trim().isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Mesaj içeriği boş olamaz.");
        }

        // 2. Kullanıcıları Bul
        User senderUser = userRepository.findById(senderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        User recipientUser = userRepository.findById(request.getRecipientId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 3. ENGELLEME KONTROLÜ (BLOCK CHECK) 🛑
        // Alıcı beni engellemiş mi?
        boolean isBlocked = userBlockRepository.existsByBlockerIdAndBlockedId(recipientUser.getId(),
                senderUser.getId());
        if (isBlocked) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST,
                    "Bu kullanıcıya mesaj gönderemezsiniz (Engellendiniz).");
        }

        // Ben onu engellemiş miyim?
        boolean iBlockedThem = userBlockRepository.existsByBlockerIdAndBlockedId(senderUser.getId(),
                recipientUser.getId());
        if (iBlockedThem) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST,
                    "Engellediğiniz bir kullanıcıya mesaj atamazsınız. Önce engeli kaldırın.");
        }

        // 4. Veritabanına Hazırlık ve Kayıt
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

        ChatMessage savedMessage = chatMessageRepository.save(message);

        // 5. WebSocket ile Canlı Gönder 🚀
        SocketMessageDto socketPayload = SocketMessageDto.builder()
                .messageId(savedMessage.getId())
                .content(savedMessage.getContent())
                .type(savedMessage.getType())
                .timestamp(savedMessage.getTimestamp())
                .senderId(senderUser.getId())
                .senderName(senderUser.getNickname())
                .senderIcon(senderUser.getProfileIcon())
                .build();

        messagingTemplate.convertAndSendToUser(
                recipientUser.getEmail(),
                "/queue/messages",
                socketPayload);

        log.info("✅ Mesaj yollandı: {} -> {}", senderUser.getEmail(), recipientUser.getEmail());

        // 6. FCM Bildirimi
        sendPushNotification(senderUser, recipientUser, request);

        return savedMessage;
    }

    // Yardımcı: Bildirim Gönderimi
    private void sendPushNotification(User sender, User recipient, ChatMessageRequest request) {
        if (recipient.getFcmToken() != null) {
            String title = sender.getNickname();
            String body = request.getType() == MessageType.IMAGE ? "📷 Bir fotoğraf gönderdi" : request.getContent();
            fcmService.sendNotification(recipient.getFcmToken(), title, body, String.valueOf(sender.getId()));
        }
    }

    @Transactional
    public GenericResponse<ChatMessage> uploadAndSendMedia(Long senderId, Long recipientId, MultipartFile file,
            MessageType type) {
        if (file.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Gönderilecek dosya boş olamaz.");
        }

        // 1. Dosyayı S3'e yükle
        String mediaUrl = s3Service.uploadFile(file);

        // 2. Mesaj isteği hazırla
        ChatMessageRequest request = new ChatMessageRequest();
        request.setRecipientId(recipientId);
        request.setContent(mediaUrl);
        request.setType(type);

        // 3. Ana metodu çağır (Engelleme kontrolü orada yapılıyor)
        ChatMessage sentMessage = processAndSendMessage(senderId, request);

        return GenericResponse.success(sentMessage);
    }

    // Son Sohbetler Listesi
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
        Pageable pageable = PageRequest.of(page, size);
        Page<ChatMessage> historyPage = chatMessageRepository.findByChatIdOrderByTimestampDesc(chatId, pageable);
        return GenericResponse.success(mapToPagedResponse(historyPage));
    }

    @Transactional
    public void markMessagesAsRead(Long currentUserId, Long otherUserId) {
        chatMessageRepository.markMessagesAsRead(otherUserId, currentUserId);
    }

    public Long getUnreadMessageCount(Long currentUserId) {
        return chatMessageRepository.countByRecipientIdAndIsReadFalse(currentUserId);
    }

    @Transactional
    public void deleteMessage(Long currentUserId, Long messageId) {
        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REQUEST, "Mesaj bulunamadı."));

        if (!message.getSenderId().equals(currentUserId)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Sadece kendi mesajlarınızı silebilirsiniz.");
        }
        chatMessageRepository.delete(message);
    }

    // Sohbet Silme (Entity ve Repo ile uyumlu String chatId)
    @Transactional
    public GenericResponse<String> deleteChat(String chatId) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        ChatMessage messageSample = chatMessageRepository.findFirstByChatId(chatId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REQUEST, "Sohbet bulunamadı."));

        // Güvenlik: Katılımcı mıyım?
        boolean isParticipant = messageSample.getSenderId().equals(currentUser.getId()) ||
                messageSample.getRecipientId().equals(currentUser.getId());

        if (!isParticipant) {
            throw new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIALS, "Bu sohbeti silme yetkiniz yok.");
        }

        chatMessageRepository.deleteAllByChatId(chatId);
        return GenericResponse.success("Sohbet başarıyla silindi.");
    }

    // "Yazıyor..." Bildirimi
    public void sendTypingNotification(Long senderId, TypingRequest request) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("senderId", senderId);
        payload.put("isTyping", request.isTyping());

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