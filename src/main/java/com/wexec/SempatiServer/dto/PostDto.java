package com.wexec.SempatiServer.dto;

import com.wexec.SempatiServer.entity.PostType;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class PostDto {
    private Long id;
    private String description;
    private PostType type;
    private LocalDateTime createdAt;
    private Double latitude;
    private Double longitude;
    private String address;
    private List<String> mediaUrls;

    private UserSummaryDto user;

    private List<CommentDto> comments;
    private List<LikeDto> likes;

    private int likeCount;
    private List<Long> taggedPetIds; // Sadece ID'ler
}