package com.wexec.SempatiServer.dto;

import com.wexec.SempatiServer.entity.Gender;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserProfileResponse {
    private Long id;
    private String email;
    private String nickname;
    private Gender gender;
    private String profileImageUrl; // Eğer user tablosuna eklersen
    // İleride buraya "sahip olduğu petler" de eklenebilir
}