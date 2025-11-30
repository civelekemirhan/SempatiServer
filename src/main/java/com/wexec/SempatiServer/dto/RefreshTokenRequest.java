package com.wexec.SempatiServer.dto;

import lombok.Data;

@Data
public class RefreshTokenRequest {
    private String token; // Frontend'in sakladığı "refreshToken" buraya gelecek
}