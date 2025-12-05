package com.wexec.SempatiServer.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL) // Null olan alanları JSON'a koyma
public class ApiError {
    private Integer status; // 400, 404 vs.
    private String message; // Hata mesajı
    private String internalCode; // AUTH_001

    private List<ValidationError> validationErrors;

    @Data
    @AllArgsConstructor
    public static class ValidationError {
        private String field;   // "email"
        private String message; // "Geçersiz format"
    }

}