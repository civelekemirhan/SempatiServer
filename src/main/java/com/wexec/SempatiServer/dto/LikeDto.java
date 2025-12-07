package com.wexec.SempatiServer.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LikeDto {
    private Long id;
    private UserSummaryDto user; // Beğenenin özeti
}