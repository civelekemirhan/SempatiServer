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

    // Entity'de chatId String olarak tanımlı.
    private String chatId;

    private Long senderId;

    private Long recipientId;

    @Column(nullable = false)
    private String content;

    @Enumerated(EnumType.STRING)
    private MessageType type;

    private LocalDateTime timestamp;

    @Builder.Default
    private boolean isRead = false;
}