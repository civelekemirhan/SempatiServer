package com.wexec.SempatiServer.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.ArrayList;
import java.util.List;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

        // 1. BusinessException (İş Mantığı Hataları)
        @ExceptionHandler(BusinessException.class)
        public ResponseEntity<GenericResponse<Object>> handleBusinessException(BusinessException ex) {
                ErrorCode errorCode = ex.getErrorCode();
                log.warn("BusinessException: {}", ex.getMessage());

                ApiError apiError = ApiError.builder()
                        .status(errorCode.getHttpStatus().value())
                        .internalCode(errorCode.getCode())
                        .message(ex.getMessage()) // Senin fırlattığın özel mesaj
                        .build();

                return ResponseEntity
                        .status(errorCode.getHttpStatus())
                        .body(GenericResponse.error(errorCode.getHttpStatus().value(), apiError));
        }

        // 2. Validasyon Hataları (GÜNCELLENDİ - Android Dostu)
        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<GenericResponse<Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {

                List<ApiError.ValidationError> validationErrors = new ArrayList<>();
                String mainMessage = "Veri doğrulama hatası."; // Varsayılan mesaj

                if (ex.getBindingResult().hasErrors()) {
                        // KRİTİK NOKTA: İlk hatayı yakala ve ana mesaj yap!
                        // Böylece Android'de direkt bu mesajı Toast olarak gösterebilirsin.
                        ObjectError firstError = ex.getBindingResult().getAllErrors().get(0);
                        mainMessage = firstError.getDefaultMessage();

                        // Detay listesini de dolduralım (Kullanmasan bile dursun)
                        ex.getBindingResult().getFieldErrors().forEach(error ->
                                validationErrors.add(new ApiError.ValidationError(error.getField(), error.getDefaultMessage()))
                        );
                }

                ApiError apiError = ApiError.builder()
                        .status(HttpStatus.BAD_REQUEST.value())
                        .internalCode(ErrorCode.VALIDATION_ERROR.getCode())
                        .message(mainMessage) // <-- Artık burası "Şifre en az 6 karakter olmalı" gibi net bir mesaj.
                        .validationErrors(validationErrors)
                        .build();

                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body(GenericResponse.error(400, apiError));
        }

        // 3. Login Hatası
        @ExceptionHandler(BadCredentialsException.class)
        public ResponseEntity<GenericResponse<Object>> handleBadCredentials(BadCredentialsException ex) {
                ApiError apiError = ApiError.builder()
                        .status(HttpStatus.UNAUTHORIZED.value())
                        .internalCode(ErrorCode.AUTH_INVALID_CREDENTIALS.getCode())
                        .message("E-posta veya şifre hatalı.")
                        .build();

                return ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body(GenericResponse.error(401, apiError));
        }

        // 3.1. Hesap Pasif
        @ExceptionHandler(DisabledException.class)
        public ResponseEntity<GenericResponse<Object>> handleDisabledException(DisabledException ex) {
                ApiError apiError = ApiError.builder()
                        .status(HttpStatus.FORBIDDEN.value())
                        .internalCode(ErrorCode.AUTH_ACCOUNT_DISABLED.getCode())
                        .message("Hesabınız henüz doğrulanmamış. Lütfen e-postanızı kontrol edin.")
                        .build();

                return ResponseEntity
                        .status(HttpStatus.FORBIDDEN)
                        .body(GenericResponse.error(403, apiError));
        }

        // 4. Genel Hatalar
        @ExceptionHandler(Exception.class)
        public ResponseEntity<GenericResponse<Object>> handleAllExceptions(Exception ex) {
                log.error("Beklenmeyen Hata: ", ex);

                ApiError apiError = ApiError.builder()
                        .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                        .internalCode(ErrorCode.INTERNAL_SERVER_ERROR.getCode())
                        .message("Sunucuda beklenmeyen bir hata oluştu.")
                        .build();

                return ResponseEntity
                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(GenericResponse.error(500, apiError));
        }
}