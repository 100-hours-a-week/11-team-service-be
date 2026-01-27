package com.thunder11.scuad.common.exception;

import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.validation.ConstraintViolationException;

import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import lombok.extern.slf4j.Slf4j;

import com.thunder11.scuad.common.response.ApiResponse;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<Object>> handleApiException(ApiException e) {
        log.error("handleApiException: ", e);

        ErrorCode ec = e.getErrorCode();

        return ResponseEntity.status(ec.getStatus())
                .body(ApiResponse.of(
                        ec.getStatus().value(),
                        ec.getCode(),
                        ec.getMessage(),
                        null
                ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidationException(MethodArgumentNotValidException e) {
        log.error("MethodArgumentNotValidException: ", e);

        Map<String, String> errors = new LinkedHashMap<>();
        for(FieldError fe : e.getBindingResult().getFieldErrors()) {
            errors.put(fe.getField(), fe.getDefaultMessage());
        }

        ErrorCode ec = ErrorCode.VALIDATION_FAILED;

        return ResponseEntity.status(ec.getStatus())
                .body(ApiResponse.of(
                        ec.getStatus().value(),
                        ec.getCode(),
                        ec.getMessage(),
                        errors
                ));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleConstraintViolationException(ConstraintViolationException e) {
        log.error("ConstraintViolationException occurred: ", e);

        ErrorCode ec = ErrorCode.VALIDATION_FAILED;

        Map<String, String> data = new LinkedHashMap<>();
        data.put("reason", e.getMessage());

        return ResponseEntity.status(ec.getStatus())
                .body(ApiResponse.of(
                        ec.getStatus().value(),
                        ec.getCode(),
                        ec.getMessage(),
                        data
                ));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Object>> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.error("HttpMessageNotReadableException occurred: ", e);

        ErrorCode ec = ErrorCode.VALIDATION_FAILED;

        return ResponseEntity.status(ec.getStatus())
                .body(ApiResponse.of(
                        ec.getStatus().value(),
                        ec.getCode(),
                        "JSON 형식이 올바르지 않습니다.",
                        null
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleException(Exception e) {
        log.error("UnhandledException occurred: ", e);

        ErrorCode ec = ErrorCode.INTERNAL_ERROR;
        return ResponseEntity.status(ec.getStatus())
                .body(ApiResponse.of(
                        ec.getStatus().value(),
                        ec.getCode(),
                        ec.getMessage(),
                        null
                ));
    }
}