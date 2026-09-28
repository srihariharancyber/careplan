package com.example.project1;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice(basePackages = "com.example.project1")
public class GlobalExceptionHandler {

    private ResponseEntity<Dtos.ApiError> build(HttpStatus s, String msg, Map<String, String> fields) {
        return ResponseEntity.status(s).body(new Dtos.ApiError(s.value(), s.getReasonPhrase(), msg, Instant.now(), fields));
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Dtos.ApiError> api(ApiException e) { return build(e.getStatus(), e.getMessage(), null); }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Dtos.ApiError> invalid(MethodArgumentNotValidException e) {
        Map<String, String> f = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors().forEach(x -> f.putIfAbsent(x.getField(), x.getDefaultMessage()));
        return build(HttpStatus.BAD_REQUEST, "Validation failed", f);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Dtos.ApiError> unreadable(HttpMessageNotReadableException e) {
        return build(HttpStatus.BAD_REQUEST, "Malformed JSON or invalid value (dates use yyyy-MM-dd, times HH:mm)", null);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Dtos.ApiError> mismatch(MethodArgumentTypeMismatchException e) {
        return build(HttpStatus.BAD_REQUEST, "Invalid value for parameter '" + e.getName() + "'", null);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Dtos.ApiError> method(HttpRequestMethodNotSupportedException e) {
        return build(HttpStatus.METHOD_NOT_ALLOWED, e.getMessage(), null);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Dtos.ApiError> noResource(NoResourceFoundException e) {
        return build(HttpStatus.NOT_FOUND, "Resource not found", null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Dtos.ApiError> other(Exception e) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error", null);
    }
}
