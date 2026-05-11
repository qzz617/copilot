package com.cmbchina.cs.assitsvc.api;

import com.cmbchina.cs.assitsvc.api.dto.ApiResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

/**
 * API 异常统一响应。
 */
@Slf4j
@RestControllerAdvice(basePackages = "com.cmbchina.cs.assitsvc.api.controller")
public class GlobalApiExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResult> handleResponseStatus(ResponseStatusException e) {
        String message = e.getReason() == null ? e.getStatus().getReasonPhrase() : e.getReason();
        return ResponseEntity.status(e.getStatus())
                .body(ApiResult.fail(String.valueOf(e.getStatus().value()), message));
    }

    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            BindException.class,
            HttpMessageNotReadableException.class
    })
    public ResponseEntity<ApiResult> handleValidation(Exception e) {
        return ResponseEntity.badRequest()
                .body(ApiResult.fail("4000", "INVALID_REQUEST", e.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResult> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.badRequest()
                .body(ApiResult.fail("4000", "INVALID_REQUEST", e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResult> handleUnexpected(Exception e) {
        log.error("[API] Unhandled API exception", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResult.fail("5000", "INTERNAL_ERROR"));
    }
}
