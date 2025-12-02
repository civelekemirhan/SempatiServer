package com.wexec.SempatiServer.common;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GenericResponse<T> {

    // İSTEDİĞİN SADE YAPI
    private int code; // HTTP Status (200, 400 vs.)
    private T payload; // Başarılıysa veri buraya (Eski adı: data)
    private ApiError error; // Hataysa detay buraya

    // Başarılı Cevap Metodu
    public static <T> GenericResponse<T> success(T payload) {
        return GenericResponse.<T>builder()
                .code(200)
                .payload(payload)
                .error(null)
                .build();
    }

    // Hata Cevap Metodu
    public static <T> GenericResponse<T> error(int httpStatus, ApiError error) {
        return GenericResponse.<T>builder()
                .code(httpStatus)
                .payload(null)
                .error(error)
                .build();
    }
}