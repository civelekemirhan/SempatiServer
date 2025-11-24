package com.wexec.SempatiServer.controller;

import com.wexec.SempatiServer.common.GenericResponse;
import com.wexec.SempatiServer.dto.AuthResponse;
import com.wexec.SempatiServer.dto.LoginRequest;
import com.wexec.SempatiServer.dto.RegisterRequest;
import com.wexec.SempatiServer.service.AuthService;
import jakarta.validation.Valid; // <-- BU İMPORT ÇOK ÖNEMLİ
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // @Valid ekledik: Artık RegisterRequest içindeki kurallara (min 6 şifre vs.) uymazsa hata fırlatır.
    @PostMapping("/register")
    public GenericResponse<String> register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/verify")
    public GenericResponse<AuthResponse> verify(
            @RequestParam String email,
            @RequestParam String code) {
        return authService.verify(email, code);
    }

    // @Valid ekledik: Login olurken de email formatı bozuksa veritabanına gitmeden reddeder.
    @PostMapping("/login")
    public GenericResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}