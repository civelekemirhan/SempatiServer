package com.wexec.SempatiServer.common;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    INTERNAL_SERVER_ERROR("GEN_001", "Sunucu kaynaklı beklenmeyen bir hata oluştu.", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_REQUEST("GEN_002", "Geçersiz istek.", HttpStatus.BAD_REQUEST),
    VALIDATION_ERROR("GEN_003", "Veri doğrulama hatası.", HttpStatus.BAD_REQUEST),
    FILE_READ_ERROR("GEN_004", "Dosya okunamadı veya bozuk.", HttpStatus.BAD_REQUEST),
    FILE_UPLOAD_ERROR("GEN_005", "Dosya sunucuya yüklenirken hata oluştu.", HttpStatus.INTERNAL_SERVER_ERROR),

    AUTH_INVALID_CREDENTIALS("AUTH_001", "E-posta veya şifre hatalı.", HttpStatus.UNAUTHORIZED),
    AUTH_EMAIL_ALREADY_EXISTS("AUTH_002", "Bu e-posta adresi zaten kullanımda.", HttpStatus.CONFLICT),
    AUTH_ACCOUNT_DISABLED("AUTH_003", "Hesabınız henüz doğrulanmamış.", HttpStatus.FORBIDDEN),
    AUTH_TOKEN_EXPIRED("AUTH_004", "Oturum süreniz dolmuş.", HttpStatus.UNAUTHORIZED),
    AUTH_INVALID_TOKEN("AUTH_005", "Geçersiz token.", HttpStatus.UNAUTHORIZED),
    AUTH_PASSWORD_MISMATCH("AUTH_006", "Şifreler uyuşmuyor.", HttpStatus.BAD_REQUEST),
    EMAIL_SEND_ERROR("MAIL_001", "E-posta gönderimi başarısız oldu.", HttpStatus.INTERNAL_SERVER_ERROR),

    POST_NOT_FOUND("POST_001", "İlgili gönderi bulunamadı.", HttpStatus.NOT_FOUND),
    NO_MORE_POSTS("POST_002", "Tüm gönderiler gösterildi.", HttpStatus.NOT_FOUND),

    REFRESH_NOT_FOUND("AUTH_007", "Refresh Token bulunamadı.", HttpStatus.UNAUTHORIZED),
    REFRESH_EXPIRED("AUTH_008", "Oturum süresi dolmuş, lütfen tekrar giriş yapın.", HttpStatus.UNAUTHORIZED),

    USER_NOT_FOUND("USER_001", "Kullanıcı bulunamadı.", HttpStatus.NOT_FOUND),
    PET_NOT_FOUND("PET_001", "İEvcil hayvan bulunamadı.", HttpStatus.NOT_FOUND),

    IMAGE_INVALID_CONTENT("IMG_001", "Yüklenen fotoğrafta kedi veya köpek tespit edilemedi.", HttpStatus.BAD_REQUEST),
    AI_SERVICE_ERROR("IMG_002", "Yapay zeka servisine erişilemedi.", HttpStatus.INTERNAL_SERVER_ERROR),

    RESET_CODE_NOT_FOUND("RESET_001", "Doğrulama kodu bulunamadı veya geçersiz.", HttpStatus.NOT_FOUND),
    RESET_CODE_EXPIRED("RESET_002", "Kodun süresi dolmuş.", HttpStatus.BAD_REQUEST),
    RESET_PASSWORD_MISMATCH("RESET_003", "Şifreler uyuşmuyor.", HttpStatus.BAD_REQUEST),
    RESET_INVALID_CODE("RESET_004", "Girdiğiniz kod hatalı.", HttpStatus.BAD_REQUEST),
    RESET_INVALID_TOKEN("RESET_005", "Geçersiz işlem tokenı.", HttpStatus.BAD_REQUEST),
    RESET_TOKEN_EXPIRED("RESET_006", "İşlem süresi dolmuş.", HttpStatus.BAD_REQUEST);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;

    ErrorCode(String code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}