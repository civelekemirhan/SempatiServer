package com.wexec.SempatiServer.controller;

import com.wexec.SempatiServer.common.GenericResponse;
import com.wexec.SempatiServer.dto.ChatMessageRequest;
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
    public void processMessage(@Payload ChatMessageRequest request, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        chatService.saveAndSendMessage(user.getId(), request);
    }

    // REST (Geçmiş)
    @GetMapping("/api/v1/chat/history/{otherUserId}")
    public GenericResponse<List<ChatMessage>> getChatHistory(@PathVariable Long otherUserId) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return chatService.getChatHistory(currentUser.getId(), otherUserId);
    }

    // --- YENİ: Medya Gönderme (HTTP POST) ---
    // Android buraya dosyayı atacak, biz S3'e yükleyip Socket'ten karşıya haber vereceğiz.
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