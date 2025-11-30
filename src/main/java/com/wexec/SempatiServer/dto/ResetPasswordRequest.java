package com.wexec.SempatiServer.dto;

import lombok.Data;

@Data
public class ResetPasswordRequest {
    private String resetToken; // Kod doğrulamasından dönen token
    private String newPassword;
}