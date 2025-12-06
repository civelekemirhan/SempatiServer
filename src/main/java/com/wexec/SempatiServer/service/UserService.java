package com.wexec.SempatiServer.service;

import com.wexec.SempatiServer.common.BusinessException;
import com.wexec.SempatiServer.common.ErrorCode;
import com.wexec.SempatiServer.common.GenericResponse;
import com.wexec.SempatiServer.dto.UserProfileResponse;
import com.wexec.SempatiServer.entity.ProfileIcon;
import com.wexec.SempatiServer.entity.User;
import com.wexec.SempatiServer.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    // 1. Kendi Profilimi Getir
    public GenericResponse<UserProfileResponse> getMyProfile() {
        User currentUser = getCurrentAuthenticatedUser();

        // DTO'daki fromEntity metodunu kullanıyoruz (Petler, Bio, Icon otomatik gelir)
        return GenericResponse.success(UserProfileResponse.fromEntity(currentUser));
    }

    // 2. Başkasının Profilini Getir (ID ile)
    public GenericResponse<UserProfileResponse> getUserProfileById(Long userId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "User ID boş olamaz.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        return GenericResponse.success(UserProfileResponse.fromEntity(user));
    }

    // 3. Profil İkonu Güncelleme
    @Transactional
    public GenericResponse<UserProfileResponse> updateProfileIcon(ProfileIcon newIcon) {
        if (newIcon == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "İkon boş olamaz.");
        }

        User currentUser = getCurrentAuthenticatedUser();

        currentUser.setProfileIcon(newIcon);
        userRepository.save(currentUser);

        return GenericResponse.success(UserProfileResponse.fromEntity(currentUser));
    }

    // --- Yardımcı Metod: O anki kullanıcıyı bul ---
    private User getCurrentAuthenticatedUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId;

        if (principal instanceof User) {
            userId = ((User) principal).getId();
        } else {
            throw new BusinessException(ErrorCode.AUTH_INVALID_TOKEN);
        }

        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}