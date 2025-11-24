package com.wexec.SempatiServer.common;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    //HATA YÖNETİMİ DAHA İYİ YAPILABİLİR

    private final String internalCode; // Örn: AUTH_001
    private final int httpStatus;      // Örn: 400, 404

    public BusinessException(String message, String internalCode, int httpStatus) {
        super(message);
        this.internalCode = internalCode;
        this.httpStatus = httpStatus;
    }

    // Hızlı kullanım için (Default 400 döner)
    public BusinessException(String message, String internalCode) {
        super(message);
        this.internalCode = internalCode;
        this.httpStatus = 400;
    }
}