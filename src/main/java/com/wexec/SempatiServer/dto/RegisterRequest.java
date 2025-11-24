package com.wexec.SempatiServer.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "E-posta adresi boş olamaz.")
    @Email(message = "Lütfen geçerli bir e-posta adresi giriniz.")
    private String email;

    @NotBlank(message = "Telefon numarası boş olamaz.")
    @Size(min = 10, max = 15, message = "Telefon numarası 10 ile 15 karakter arasında olmalıdır.")
    private String phoneNumber;

    @NotBlank(message = "Kullanıcı adı boş olamaz.")
    @Size(min = 3, message = "Kullanıcı adı en az 3 karakter olmalıdır.")
    private String nickname;

    @NotBlank(message = "Şifre boş olamaz.")
    @Size(min = 6, message = "Şifre en az 6 karakter olmalıdır.")
    private String password;

    @NotBlank(message = "Şifre tekrarı boş olamaz.")
    private String confirmPassword;
}