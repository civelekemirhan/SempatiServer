package com.wexec.SempatiServer.controller;

import com.wexec.SempatiServer.common.GenericResponse;
import com.wexec.SempatiServer.dto.UserProfileResponse;
import com.wexec.SempatiServer.entity.ProfileIcon;
import com.wexec.SempatiServer.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // Kendi profilim
    @GetMapping("/me")
    public GenericResponse<UserProfileResponse> getMyProfile() {
        return userService.getMyProfile();
    }

    // Başkasının profili (ID ile)
    @GetMapping("/{userId}")
    public GenericResponse<UserProfileResponse> getUserProfileById(@PathVariable Long userId) {
        return userService.getUserProfileById(userId);
    }

    // İkon Güncelleme
    // Kullanımı: POST /api/v1/users/icon?icon=ICON1
    @PatchMapping("/icon")
    public GenericResponse<UserProfileResponse> updateProfileIcon(@RequestParam ProfileIcon icon) {
        return userService.updateProfileIcon(icon);
    }
}