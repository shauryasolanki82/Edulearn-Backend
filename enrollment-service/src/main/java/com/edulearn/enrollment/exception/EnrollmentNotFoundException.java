package com.edulearn.enrollment.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class EnrollmentNotFoundException extends RuntimeException {
    public EnrollmentNotFoundException(String msg) { super(msg); }
}
