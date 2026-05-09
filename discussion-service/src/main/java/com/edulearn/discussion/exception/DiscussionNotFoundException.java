package com.edulearn.discussion.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class DiscussionNotFoundException extends RuntimeException {
    public DiscussionNotFoundException(String m) { super(m); }
}
