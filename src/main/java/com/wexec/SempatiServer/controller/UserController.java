package com.wexec.SempatiServer.controller;

import com.wexec.SempatiServer.common.GenericResponse;
import com.wexec.SempatiServer.dto.PetDto;
import com.wexec.SempatiServer.dto.UserProfileResponse;
import com.wexec.SempatiServer.dto.UserUpdateRequest; // YENİ DTO İMPORTU
import com.wexec.SempatiServer.entity.ProfileIcon;
import com.wexec.SempatiServer.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public GenericResponse<UserProfileResponse> getMyProfile() {
        return userService.getMyProfile();
    }

    @GetMapping("/myPets")
    public GenericResponse<List<PetDto>> getMyPets() {
        return userService.getMyPets();
    }

    @GetMapping("/{userId}")
    public GenericResponse<UserProfileResponse> getUserProfileById(@PathVariable Long userId) {
        return userService.getUserProfileById(userId);
    }

    @PatchMapping("/me")
    public GenericResponse<UserProfileResponse> updateMyProfile(@RequestBody UserUpdateRequest request) {
        return userService.updateUserProfile(request);
    }

    @PatchMapping("/icon")
    public GenericResponse<UserProfileResponse> updateProfileIcon(@RequestParam ProfileIcon icon) {
        return userService.updateProfileIcon(icon);
    }
}