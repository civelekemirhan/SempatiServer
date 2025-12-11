package com.wexec.SempatiServer.dto;

import com.wexec.SempatiServer.entity.Gender;
import com.wexec.SempatiServer.entity.ProfileIcon;
import lombok.Data;

@Data
public class UserUpdateRequest {
    private String nickname;
    private String bio;
    private Gender gender;
    private ProfileIcon profileIcon;
}