package com.wexec.SempatiServer.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserProfileResponse {
    private Long id;
    private String email;
    private String nickname;
    private String phoneNumber;
    private String profileImageUrl; // Eğer user tablosuna eklersen
    // İleride buraya "sahip olduğu petler" de eklenebilir
}