package com.wexec.SempatiServer.common;


import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ApiError {
    private String message;
    private String internalCode; // Hata takibi için kod (örn: AUTH_001)
}