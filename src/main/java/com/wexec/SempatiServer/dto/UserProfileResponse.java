package com.wexec.SempatiServer.dto;

import com.wexec.SempatiServer.entity.Gender;
import com.wexec.SempatiServer.entity.ProfileIcon;
import com.wexec.SempatiServer.entity.User;
import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

@Data
@Builder
public class UserProfileResponse {
    private Long id;
    private String email;
    private String nickname;
    private String bio;
    private Gender gender;
    private ProfileIcon profileIcon; // YENİ
    private List<PetDto> pets;

    public static UserProfileResponse fromEntity(User user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .bio(user.getBio())
                .gender(user.getGender())
                .profileIcon(user.getProfileIcon()) // Enum dönüyoruz
                .pets(user.getPets() != null ?
                        user.getPets().stream().map(PetDto::fromEntity).collect(Collectors.toList())
                        : new ArrayList<>())
                .build();
    }
}