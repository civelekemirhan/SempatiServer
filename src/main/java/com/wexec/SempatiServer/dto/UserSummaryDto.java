package com.wexec.SempatiServer.dto;

import com.wexec.SempatiServer.entity.ProfileIcon;
import com.wexec.SempatiServer.entity.User;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserSummaryDto {
    private Long id;
    private String nickname;
    private ProfileIcon profileIcon;

    public static UserSummaryDto fromEntity(User user) {
        return UserSummaryDto.builder()
                .id(user.getId())
                .nickname(user.getNickname())
                .profileIcon(user.getProfileIcon())
                .build();
    }
}