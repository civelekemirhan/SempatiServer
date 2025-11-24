package com.wexec.SempatiServer.dto;

import com.wexec.SempatiServer.entity.MessageType;
import lombok.Data;

@Data
public class ChatMessageRequest {
    private Long recipientId;
    private String content;
    private MessageType type = MessageType.TEXT; // Varsayılan TEXT
}