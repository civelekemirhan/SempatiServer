package com.wexec.SempatiServer.controller;

import com.wexec.SempatiServer.common.GenericResponse;
import com.wexec.SempatiServer.dto.*;
import com.wexec.SempatiServer.service.AuthService;
import com.wexec.SempatiServer.service.PasswordResetService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService; // Yeni servisi buraya ekledik

    // --- MEVCUT AUTH İŞLEMLERİ ---

    @PostMapping("/register")
    public GenericResponse<String> register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    // Bu endpoint kayıt (register) sonrası doğrulama içindir
    @PostMapping("/verify")
    public GenericResponse<AuthResponse> verify(
            @RequestParam String email,
            @RequestParam String code) {
        return authService.verify(email, code);
    }

    @PostMapping("/login")
    public GenericResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh-token")
    public GenericResponse<AuthResponse> refreshToken(@RequestBody RefreshTokenRequest request) {
        return authService.refreshToken(request);
    }

    // --- YENİ ŞİFRE SIFIRLAMA AKIŞI (SecurityConfig ile uyumlu isimler) ---

    // 1. Adım
    @PostMapping("/forgot-password")
    public GenericResponse<String> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        passwordResetService.initiatePasswordReset(request);
        return GenericResponse.success("Doğrulama kodu e-posta adresinize gönderildi.");
    }

    // 2. Adım
    @PostMapping("/verify-reset-code")
    public GenericResponse<ForgotPasswordVerifyResponse> verifyResetCode(@RequestBody VerifyCodeRequest request) {
        return GenericResponse.success(passwordResetService.verifyCode(request));
    }

    // 3. Adım
    @PatchMapping("/reset-password")
    public GenericResponse<String> resetPassword(@RequestBody ResetPasswordRequest request) {
        passwordResetService.updatePassword(request);
        return GenericResponse.success("Şifreniz başarıyla güncellendi.");
    }
}