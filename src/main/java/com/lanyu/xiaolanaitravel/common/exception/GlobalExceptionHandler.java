package com.lanyu.xiaolanaitravel.common.exception;

import com.lanyu.xiaolanaitravel.common.dto.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiErrorResponse> handleStatus(
            ResponseStatusException exception, HttpServletRequest request) {
        int status = exception.getStatusCode().value();
        String message = exception.getReason() == null ? "请求处理失败" : exception.getReason();
        return ResponseEntity.status(status).body(error(status, message, request));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleBodyValidation(
            MethodArgumentNotValidException exception, HttpServletRequest request) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst().map(error -> error.getDefaultMessage()).orElse("请求参数不正确");
        return ResponseEntity.badRequest().body(error(400, message, request));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleParameterValidation(
            ConstraintViolationException exception, HttpServletRequest request) {
        String message = exception.getConstraintViolations().stream()
                .findFirst().map(violation -> violation.getMessage()).orElse("请求参数不正确");
        return ResponseEntity.badRequest().body(error(400, message, request));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiErrorResponse> handleMalformedRequest(
            Exception exception, HttpServletRequest request) {
        return ResponseEntity.badRequest().body(error(400, "请求格式不正确", request));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataConflict(
            DataIntegrityViolationException exception, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(error(409, "数据已存在或与当前状态冲突", request));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(
            Exception exception, HttpServletRequest request) {
        LOGGER.error("Unhandled request failure: {} {}", request.getMethod(),
                request.getRequestURI(), exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(error(500, "服务器处理请求失败", request));
    }

    private ApiErrorResponse error(int status, String message, HttpServletRequest request) {
        return new ApiErrorResponse(status, message, request.getRequestURI(), LocalDateTime.now());
    }
}
