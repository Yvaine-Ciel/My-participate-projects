package com.coview.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatus(ResponseStatusException ex) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        return ResponseEntity.status(status).body(errorBody(status, ex.getReason()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.groupingBy(FieldError::getField,
                        LinkedHashMap::new,
                        Collectors.mapping(FieldError::getDefaultMessage, Collectors.joining(", "))))
                .entrySet()
                .stream()
                .map(entry -> entry.getKey() + ": " + entry.getValue())
                .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest().body(errorBody(HttpStatus.BAD_REQUEST, message));
    }

    private Map<String, Object> errorBody(HttpStatus status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("error", statusText(status));
        body.put("message", message == null || message.isBlank() ? statusText(status) : message);
        return body;
    }

    private String statusText(HttpStatus status) {
        return switch (status) {
            case BAD_REQUEST -> "请求参数不正确";
            case FORBIDDEN -> "没有权限执行该操作";
            case NOT_FOUND -> "资源不存在";
            case METHOD_NOT_ALLOWED -> "请求方式不支持";
            case CONFLICT -> "请求存在冲突";
            case INTERNAL_SERVER_ERROR -> "服务器处理失败";
            default -> status.getReasonPhrase();
        };
    }
}
