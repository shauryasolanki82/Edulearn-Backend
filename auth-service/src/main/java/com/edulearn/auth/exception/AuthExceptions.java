package com.edulearn.auth.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

// ── Resource Not Found ────────────────────────────────────────────────────────

@ResponseStatus(HttpStatus.NOT_FOUND)
class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}

// ── Email Already Exists ──────────────────────────────────────────────────────

@ResponseStatus(HttpStatus.CONFLICT)
class EmailAlreadyExistsException extends RuntimeException {
    public EmailAlreadyExistsException(String message) {
        super(message);
    }
}

// ── Invalid Credentials ───────────────────────────────────────────────────────

@ResponseStatus(HttpStatus.UNAUTHORIZED)
class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException(String message) {
        super(message);
    }
}

// ── Invalid Token ─────────────────────────────────────────────────────────────

@ResponseStatus(HttpStatus.UNAUTHORIZED)
class InvalidTokenException extends RuntimeException {
    public InvalidTokenException(String message) {
        super(message);
    }
}

// ── Account Disabled ──────────────────────────────────────────────────────────

@ResponseStatus(HttpStatus.FORBIDDEN)
class AccountDisabledException extends RuntimeException {
    public AccountDisabledException(String message) {
        super(message);
    }
}
