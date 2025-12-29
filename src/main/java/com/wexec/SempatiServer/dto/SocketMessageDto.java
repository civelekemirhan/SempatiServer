package com.wexec.SempatiServer.dto;

import com.wexec.SempatiServer.entity.MessageType;
import com.wexec.SempatiServer.entity.ProfileIcon;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class SocketMessageDto {
    private Long messageId;
    private String content;
    private MessageType type;
    private LocalDateTime timestamp;
    private Long senderId;
    private String senderName;
    private ProfileIcon senderIcon;
}
