package com.wexec.SempatiServer.dto;

import com.wexec.SempatiServer.entity.Gender;

import com.wexec.SempatiServer.entity.User;
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

    public static UserProfileResponse fromEntity(User user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .gender(user.getGender()) // User tablosunda gender alanı varsa
                // .profileImageUrl(user.getProfilePictureUrl()) // Varsa ekle
                .build();
    }
}