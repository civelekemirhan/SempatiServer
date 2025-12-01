package com.wexec.SempatiServer.common;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GenericResponse<T> {
    private boolean success; // Frontend için kolaylık (true/false)
    private String message; // Genel bilgi mesajı
    private T data; // Başarılı veri (Payload ismini data yaptım, standarttır)
    private ApiError error; // Hata detayı

    public static <T> GenericResponse<T> success(T data) {
        return GenericResponse.<T>builder()
                .success(true)
                .code(200) // builder metoduna eklenmeli veya aşağıda field olarak tanımlanmalı
                .data(data)
                .build();
    }

    // Mevcut kodunda "code" alanı class seviyesinde vardı, onu koruyoruz:
    private int code;

    public static <T> GenericResponse<T> success(T data, String message) {
        return GenericResponse.<T>builder()
                .success(true)
                .code(200)
                .message(message)
                .data(data)
                .build();
    }

    public static <T> GenericResponse<T> error(ApiError error) {
        return GenericResponse.<T>builder()
                .success(false)
                .code(error.getStatus())
                .error(error)
                .build();
    }
}