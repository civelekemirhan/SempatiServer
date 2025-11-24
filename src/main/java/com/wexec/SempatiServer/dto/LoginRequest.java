package com.wexec.SempatiServer.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank(message = "E-posta boş olamaz.")
    @Email(message = "Geçersiz e-posta formatı.")
    private String email;

    @NotBlank(message = "Şifre boş olamaz.")
    private String password;
}