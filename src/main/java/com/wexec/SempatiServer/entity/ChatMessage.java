package com.wexec.SempatiServer.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "chat_messages")
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String chatId;
    private Long senderId;
    private Long recipientId;

    @Column(nullable = false)
    private String content; // Yazı ise metin, Medya ise S3 Linki (URL) burada duracak.

    @Enumerated(EnumType.STRING)
    private MessageType type; // TEXT, IMAGE, AUDIO

    private LocalDateTime timestamp;

    @Builder.Default
    private boolean isRead = false; // Varsayılan: Okunmadı
}