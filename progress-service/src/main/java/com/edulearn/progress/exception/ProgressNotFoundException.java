package com.edulearn.progress.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ProgressNotFoundException extends RuntimeException {
    public ProgressNotFoundException(String m) { super(m); }
}
