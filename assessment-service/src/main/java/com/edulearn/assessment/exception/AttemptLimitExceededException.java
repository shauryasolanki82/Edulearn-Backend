package com.edulearn.assessment.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class AttemptLimitExceededException extends RuntimeException {
    public AttemptLimitExceededException(String m) { super(m); }
}
