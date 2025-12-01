package com.wexec.SempatiServer.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j // Loglama için gerekli (Konsola hata basar)
public class GlobalExceptionHandler {

        // 1. BusinessException (Bizim fırlattığımız kontrollü iş mantığı hataları)
        @ExceptionHandler(BusinessException.class)
        public ResponseEntity<GenericResponse<Object>> handleBusinessException(BusinessException ex) {
                ErrorCode errorCode = ex.getErrorCode();

                // Hangi hatayı aldığımızı loglayalım (Debug kolaylığı için)
                log.warn("BusinessException fırlatıldı: Kod: {}, Mesaj: {}", errorCode.getCode(), ex.getMessage());

                ApiError apiError = ApiError.builder()
                                .status(errorCode.getHttpStatus().value())
                                .internalCode(errorCode.getCode())
                                .message(ex.getMessage()) // Enum mesajı veya özel override edilmiş mesaj
                                .build();

                return ResponseEntity
                                .status(errorCode.getHttpStatus())
                                .body(GenericResponse.error(apiError));
        }

        // 2. Validasyon Hataları (DTO'daki @NotNull, @Email vb. ihlalleri)
        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<GenericResponse<Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
                Map<String, String> errors = new HashMap<>();

                // Tüm hataları topla: { "email": "Geçersiz format", "password": "Çok kısa" }
                ex.getBindingResult().getAllErrors().forEach((error) -> {
                        String fieldName = ((FieldError) error).getField();
                        String errorMessage = error.getDefaultMessage();
                        errors.put(fieldName, errorMessage);
                });

                log.warn("Validasyon Hatası: {}", errors);

                ApiError apiError = ApiError.builder()
                                .status(HttpStatus.BAD_REQUEST.value())
                                .internalCode(ErrorCode.VALIDATION_ERROR.getCode())
                                .message("Girdiğiniz verilerde eksik veya hatalı alanlar var.")
                                .validationErrors(errors) // Hata detaylarını Map olarak ekliyoruz
                                .build();

                return ResponseEntity
                                .status(HttpStatus.BAD_REQUEST)
                                .body(GenericResponse.error(apiError));
        }

        // 3. Spring Security Login Hataları (Yanlış şifre/email)
        @ExceptionHandler(BadCredentialsException.class)
        public ResponseEntity<GenericResponse<Object>> handleBadCredentials(BadCredentialsException ex) {
                // Güvenlik: Detay vermeden sadece "Hatalı giriş" diyoruz
                log.warn("Hatalı giriş denemesi: {}", ex.getMessage());

                ApiError apiError = ApiError.builder()
                                .status(HttpStatus.UNAUTHORIZED.value())
                                .internalCode(ErrorCode.AUTH_INVALID_CREDENTIALS.getCode())
                                .message(ErrorCode.AUTH_INVALID_CREDENTIALS.getMessage())
                                .build();

                return ResponseEntity
                                .status(HttpStatus.UNAUTHORIZED)
                                .body(GenericResponse.error(apiError));
        }

        @ExceptionHandler(DisabledException.class)
        public ResponseEntity<GenericResponse<Object>> handleDisabledException(DisabledException ex) {
                log.warn("Devre dışı hesap ile giriş denemesi: {}", ex.getMessage());

                ApiError apiError = ApiError.builder()
                                .status(HttpStatus.FORBIDDEN.value())
                                .internalCode(ErrorCode.AUTH_ACCOUNT_DISABLED.getCode())
                                .message(ErrorCode.AUTH_ACCOUNT_DISABLED.getMessage())
                                .build();

                return ResponseEntity
                                .status(HttpStatus.FORBIDDEN)
                                .body(GenericResponse.error(apiError));
        }

        // 4. Beklenmeyen Genel Hatalar (500 - NullPointer, DB Connection, SQL Syntax
        // vb.)
        @ExceptionHandler(Exception.class)
        public ResponseEntity<GenericResponse<Object>> handleAllExceptions(Exception ex) {
                // Hatanın gerçek sebebini (Stack Trace) SADECE LOGA YAZ. Kullanıcı görmesin.
                log.error("Beklenmeyen Kritik Hata: ", ex);

                // Kullanıcıya genel ve güvenli bir mesaj dön
                ApiError apiError = ApiError.builder()
                                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                                .internalCode(ErrorCode.INTERNAL_SERVER_ERROR.getCode())
                                .message(ErrorCode.INTERNAL_SERVER_ERROR.getMessage())
                                // ÖNEMLİ: ex.getMessage() buraya koyulmaz, güvenlik açığı yaratır.
                                .build();

                return ResponseEntity
                                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(GenericResponse.error(apiError));
        }
}