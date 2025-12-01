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
        User principal = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User currentUser = userRepository.findById(principal.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        UserProfileResponse response = UserProfileResponse.builder()
                .id(currentUser.getId())
                .email(currentUser.getEmail())
                .nickname(currentUser.getNickname())
                .gender(currentUser.getGender())
                .build();

        return GenericResponse.success(response);
    }
}