package com.wexec.SempatiServer.common;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 1. Bizim fırlattığımız özel BusinessException'ları yakalar
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<GenericResponse<Object>> handleBusinessException(BusinessException ex) {
        return ResponseEntity
                .status(ex.getHttpStatus())
                .body(GenericResponse.error(ex.getHttpStatus(), ex.getMessage(), ex.getInternalCode()));
    }

    // 2. @Valid anotasyonu ile yapılan validasyon hatalarını yakalar (Örn: Email formatı bozuk)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<GenericResponse<Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        // Hataları topla (Örn: {email: "Email boş olamaz", password: "En az 6 karakter"})
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        // İlk hatayı mesaj olarak dönelim veya map'i stringe çevirelim
        String firstErrorMessage = errors.values().stream().findFirst().orElse("Validasyon hatası");

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(GenericResponse.error(400, firstErrorMessage, "VALIDATION_ERR"));
    }

    // 3. Spring Security Login hatalarını yakalar
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<GenericResponse<Object>> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(GenericResponse.error(401, "Kullanıcı adı veya şifre hatalı", "AUTH_FAIL"));
    }

    // 4. Beklenmeyen tüm diğer hataları yakalar (NullPointer, DB connection vb.)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<GenericResponse<Object>> handleAllExceptions(Exception ex) {
        // Loglama yapılabilir: log.error("Bilinmeyen hata", ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(GenericResponse.error(500, "Beklenmeyen bir sunucu hatası oluştu: " + ex.getMessage(), "INTERNAL_ERR"));
    }
}