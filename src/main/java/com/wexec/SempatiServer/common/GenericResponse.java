package com.wexec.SempatiServer.common;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GenericResponse<T> {
    private int code;       // HTTP Status (200, 400 vs.)
    private T payload;      // Başarılıysa veri buraya
    private ApiError error; // Hataysa detay buraya

    public static <T> GenericResponse<T> success(T payload) {
        return GenericResponse.<T>builder()
                .code(200)
                .payload(payload)
                .error(null)
                .build();
    }

    public static <T> GenericResponse<T> error(int httpStatus, String message, String internalCode) {
        return GenericResponse.<T>builder()
                .code(httpStatus)
                .payload(null)
                .error(new ApiError(message, internalCode))
                .build();
    }
}