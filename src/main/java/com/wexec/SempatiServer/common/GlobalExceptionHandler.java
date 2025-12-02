package com.wexec.SempatiServer.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
@SuppressWarnings("null")
public class GlobalExceptionHandler {

        // 1. BusinessException (Özel İş Mantığı Hataları)
        @ExceptionHandler(BusinessException.class)
        public ResponseEntity<GenericResponse<Object>> handleBusinessException(BusinessException ex) {
                ErrorCode errorCode = ex.getErrorCode();

                log.warn("BusinessException: Kod: {}, Mesaj: {}", errorCode.getCode(), ex.getMessage());

                ApiError apiError = ApiError.builder()
                                .status(errorCode.getHttpStatus().value())
                                .internalCode(errorCode.getCode())
                                .message(ex.getMessage())
                                .build();

                // GenericResponse.error(int code, ApiError error) yapısına uygun çağrı:
                return ResponseEntity
                                .status(errorCode.getHttpStatus())
                                .body(GenericResponse.error(errorCode.getHttpStatus().value(), apiError));
        }

        // 2. Validasyon Hataları (@Valid ile yakalananlar)
        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<GenericResponse<Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
                Map<String, String> errors = new HashMap<>();

                // Tüm alan hatalarını topla
                ex.getBindingResult().getAllErrors().forEach((error) -> {
                        String fieldName = ((FieldError) error).getField();
                        String errorMessage = error.getDefaultMessage();
                        errors.put(fieldName, errorMessage);
                });

                log.warn("Validasyon Hatası: {}", errors);

                ApiError apiError = ApiError.builder()
                                .status(HttpStatus.BAD_REQUEST.value())
                                .internalCode(ErrorCode.VALIDATION_ERROR.getCode())
                                .message("Veri doğrulama hatası")
                                .validationErrors(errors) // Hangi alanların hatalı olduğunu ekler
                                .build();

                return ResponseEntity
                                .status(HttpStatus.BAD_REQUEST)
                                .body(GenericResponse.error(400, apiError));
        }

        // 3. Login Hatası (Yanlış Şifre)
        @ExceptionHandler(BadCredentialsException.class)
        public ResponseEntity<GenericResponse<Object>> handleBadCredentials(BadCredentialsException ex) {
                log.warn("Hatalı giriş denemesi: {}", ex.getMessage());

                ApiError apiError = ApiError.builder()
                                .status(HttpStatus.UNAUTHORIZED.value())
                                .internalCode(ErrorCode.AUTH_INVALID_CREDENTIALS.getCode())
                                .message(ErrorCode.AUTH_INVALID_CREDENTIALS.getMessage())
                                .build();

                return ResponseEntity
                                .status(HttpStatus.UNAUTHORIZED)
                                .body(GenericResponse.error(401, apiError));
        }

        // 3.1. Hesap Pasif Hatası (Mail Doğrulanmamış)
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
                                .body(GenericResponse.error(403, apiError));
        }

        // 4. Genel Beklenmeyen Hatalar (500)
        @ExceptionHandler(Exception.class)
        public ResponseEntity<GenericResponse<Object>> handleAllExceptions(Exception ex) {
                // Hatanın detayını (SQL, NullPointer vs.) sadece loga yaz, kullanıcıya gösterme
                log.error("Beklenmeyen Kritik Hata: ", ex);

                ApiError apiError = ApiError.builder()
                                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                                .internalCode(ErrorCode.INTERNAL_SERVER_ERROR.getCode())
                                .message(ErrorCode.INTERNAL_SERVER_ERROR.getMessage())
                                .build();

                return ResponseEntity
                                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(GenericResponse.error(500, apiError));
        }
}