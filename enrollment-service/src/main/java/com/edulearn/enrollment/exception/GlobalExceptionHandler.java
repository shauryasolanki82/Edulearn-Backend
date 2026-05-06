package com.edulearn.enrollment.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.*;

@RestControllerAdvice @Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String,String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(e -> errors.put(((FieldError)e).getField(), e.getDefaultMessage()));
        return ResponseEntity.badRequest().body(new ErrorResponse(400,"Validation failed",errors.toString(),LocalDateTime.now()));
    }
    @ExceptionHandler(EnrollmentNotFoundException.class)
    public ResponseEntity<ErrorResponse> notFound(EnrollmentNotFoundException ex) { return build(HttpStatus.NOT_FOUND,ex.getMessage()); }
    @ExceptionHandler(AlreadyEnrolledException.class)
    public ResponseEntity<ErrorResponse> conflict(AlreadyEnrolledException ex) { return build(HttpStatus.CONFLICT,ex.getMessage()); }
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> illegalState(IllegalStateException ex) { return build(HttpStatus.BAD_REQUEST,ex.getMessage()); }
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> access(AccessDeniedException ex) { return build(HttpStatus.FORBIDDEN,"Access denied"); }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> general(Exception ex) { log.error("Error: {}",ex.getMessage(),ex); return build(HttpStatus.INTERNAL_SERVER_ERROR,"Unexpected error"); }

    private ResponseEntity<ErrorResponse> build(HttpStatus s, String msg) {
        return ResponseEntity.status(s).body(new ErrorResponse(s.value(),s.getReasonPhrase(),msg,LocalDateTime.now()));
    }
    public record ErrorResponse(int status, String error, String message, LocalDateTime timestamp) {}
}
