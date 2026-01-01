package com.wexec.SempatiServer.controller;

import com.wexec.SempatiServer.common.GenericResponse;
import com.wexec.SempatiServer.dto.ChatMessageRequest;
import com.wexec.SempatiServer.dto.ChatSummaryDto;
import com.wexec.SempatiServer.dto.PagedResponse;
import com.wexec.SempatiServer.dto.TypingRequest;
import com.wexec.SempatiServer.entity.ChatMessage;
import com.wexec.SempatiServer.entity.MessageType;
import com.wexec.SempatiServer.entity.User;
import com.wexec.SempatiServer.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    // WebSocket (Sadece Text Mesajlar için)
    @MessageMapping("/chat")
    public void processMessage(@Payload ChatMessageRequest request) {
        chatService.saveAndSendMessage(request.getSenderId(), request);
    }

    // REST (Geçmiş)
    @GetMapping("/api/v1/chat/history/{otherUserId}")
    public GenericResponse<PagedResponse<ChatMessage>> getChatHistory(
            @PathVariable Long otherUserId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return chatService.getChatHistory(currentUser.getId(), otherUserId, page, size);
    }

    // Son Sohbetler
    @GetMapping("/api/v1/chat/recent")
    public GenericResponse<List<ChatSummaryDto>> getRecentChats() {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return chatService.getRecentChats(currentUser.getId());
    }

    // --- YENİ: Mesajları Okundu Yapma ---
    // Kullanıcı bir sohbeti açtığında bu endpoint tetiklenmeli.
    // otherUserId: Kiminle olan sohbet açıldıysa onun ID'si.
    @PatchMapping("/api/v1/chat/read/{otherUserId}")
    public GenericResponse<String> markMessagesAsRead(@PathVariable Long otherUserId) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        chatService.markMessagesAsRead(currentUser.getId(), otherUserId);
        return GenericResponse.success("Mesajlar okundu olarak işaretlendi.");
    }

    // --- YENİ: Toplam Okunmamış Mesaj Sayısı ---
    // Alt navigasyon barında "Chat (3)" gibi göstermek için.
    @GetMapping("/api/v1/chat/unread/count")
    public GenericResponse<Long> getTotalUnreadCount() {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long count = chatService.getUnreadMessageCount(currentUser.getId());

        return GenericResponse.success(count);
    }

    // --- YENİ: Tekil Mesaj Silme ---
    @DeleteMapping("/api/v1/chat/message/{messageId}")
    public GenericResponse<String> deleteMessage(@PathVariable Long messageId) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        chatService.deleteMessage(currentUser.getId(), messageId);
        return GenericResponse.success("Mesaj silindi.");
    }

    // Sohbeti Sil
    // URL: DELETE /api/v1/chats/{chatId}
    @DeleteMapping("/{chatId}")
    // DÜZELTME: @PathVariable Long değil String olmalı
    public GenericResponse<String> deleteChat(@PathVariable String chatId) {
        return chatService.deleteChat(chatId);
    }

    // --- YENİ: "Yazıyor..." Bilgisi (WebSocket) ---
    // Android tarafı: /app/typing hedefine {recipientId: 5, isTyping: true}
    // gönderir.
    @MessageMapping("/typing")
    public void sendTypingStatus(@Payload TypingRequest request, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        // Bu bilgiyi direkt karşı tarafa iletiyoruz, veritabanına kaydetmeye gerek yok.
        chatService.sendTypingNotification(user.getId(), request);
    }

    // --- YENİ: Medya Gönderme (HTTP POST) ---
    // Android buraya dosyayı atacak, biz S3'e yükleyip Socket'ten karşıya haber
    // vereceğiz.
    @PostMapping(value = "/api/v1/chat/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public GenericResponse<ChatMessage> sendMediaMessage(
            @RequestParam("recipientId") Long recipientId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("type") MessageType type // IMAGE veya AUDIO
    ) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return chatService.uploadAndSendMedia(currentUser.getId(), recipientId, file, type);
    }
}