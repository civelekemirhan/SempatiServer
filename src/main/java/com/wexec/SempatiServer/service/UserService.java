package com.wexec.SempatiServer.service;

import com.wexec.SempatiServer.common.BusinessException;
import com.wexec.SempatiServer.common.ErrorCode;
import com.wexec.SempatiServer.common.GenericResponse;
import com.wexec.SempatiServer.dto.UserProfileResponse;
import com.wexec.SempatiServer.entity.User;
import com.wexec.SempatiServer.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public GenericResponse<UserProfileResponse> getMyProfile() {
        // O an token ile giriş yapmış kullanıcıyı SecurityContext'ten al
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        Long userId;
        if (principal instanceof User) {
            userId = ((User) principal).getId();
        } else {
            // Nadir durum: Principal User tipinde değilse (örn: String username)
            throw new BusinessException(ErrorCode.AUTH_INVALID_TOKEN);
        }

        // Null Safety Check: ID null ise token geçersiz sayılır (IDE uyarısını çözer)
        if (userId == null) {
            throw new BusinessException(ErrorCode.AUTH_INVALID_TOKEN);
        }

        // Veritabanından güncel halini çek.
        // Eğer kullanıcı bulunamazsa (örn: silinmişse) BusinessException fırlat.
        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        UserProfileResponse response = UserProfileResponse.builder()
                .id(currentUser.getId())
                .email(currentUser.getEmail())
                .nickname(currentUser.getNickname())
                .gender(currentUser.getGender())
                .build();

        // GenericResponse yapısı değişti ama .success() metodu hala aynı imzaya sahip.
        // Arka planda veriyi 'payload' alanına koyacak.
        return GenericResponse.success(response);
    }
}