package com.wexec.SempatiServer.common;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GenericResponse<T> {

    private int code;
    private T payload;
    private ApiError error;

    public static <T> GenericResponse<T> success(T payload) {
        return GenericResponse.<T>builder()
                .code(200)
                .payload(payload)
                .error(null)
                .build();
    }

    public static <T> GenericResponse<T> error(int httpStatus, ApiError error) {
        return GenericResponse.<T>builder()
                .code(httpStatus)
                .payload(null)
                .error(error)
                .build();
    }
}