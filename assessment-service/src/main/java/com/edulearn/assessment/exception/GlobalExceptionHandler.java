package com.edulearn.assessment.exception;

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
    public ResponseEntity<ErrorResponse> validation(MethodArgumentNotValidException ex) {
        Map<String,String> e = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(err -> e.put(((FieldError)err).getField(), err.getDefaultMessage()));
        return ResponseEntity.badRequest().body(new ErrorResponse(400,"Validation failed",e.toString(),LocalDateTime.now()));
    }
    @ExceptionHandler(QuizNotFoundException.class)
    public ResponseEntity<ErrorResponse> quizNotFound(QuizNotFoundException ex) { return build(HttpStatus.NOT_FOUND, ex.getMessage()); }
    @ExceptionHandler(QuestionNotFoundException.class)
    public ResponseEntity<ErrorResponse> qNotFound(QuestionNotFoundException ex) { return build(HttpStatus.NOT_FOUND, ex.getMessage()); }
    @ExceptionHandler(AttemptLimitExceededException.class)
    public ResponseEntity<ErrorResponse> limit(AttemptLimitExceededException ex) { return build(HttpStatus.CONFLICT, ex.getMessage()); }
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> access(AccessDeniedException ex) { return build(HttpStatus.FORBIDDEN, "Access denied"); }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> general(Exception ex) { log.error("{}", ex.getMessage(), ex); return build(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error"); }
    private ResponseEntity<ErrorResponse> build(HttpStatus s, String m) {
        return ResponseEntity.status(s).body(new ErrorResponse(s.value(), s.getReasonPhrase(), m, LocalDateTime.now()));
    }
    public record ErrorResponse(int status, String error, String message, LocalDateTime timestamp) {}
}
