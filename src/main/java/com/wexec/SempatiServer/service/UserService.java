package com.wexec.SempatiServer.service;

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
        // O an token ile giriş yapmış kullanıcıyı al
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        // Güncel veriyi DB'den çekmek her zaman daha güvenlidir
        User currentUser = userRepository.findById(user.getId()).orElseThrow();

        UserProfileResponse response = UserProfileResponse.builder()
                .id(currentUser.getId())
                .email(currentUser.getEmail())
                .nickname(currentUser.getNickname())
                .phoneNumber(currentUser.getPhoneNumber())
                .build();

        return GenericResponse.success(response);
    }
}