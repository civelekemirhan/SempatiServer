package com.wexec.SempatiServer.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class CommentDto {
    private Long id;
    private String text;
    private LocalDateTime createdAt;
    private UserProfileResponse user; // Yorumu yapanın özeti
}