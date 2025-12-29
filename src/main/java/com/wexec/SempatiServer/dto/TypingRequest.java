package com.wexec.SempatiServer.dto;

import lombok.Data;

@Data
public class TypingRequest {
    private Long recipientId; // Kime yazıyorum?
    private boolean isTyping; // Yazıyor mu (true) durdu mu (false)?
}