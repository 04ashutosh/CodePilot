package com.codepilot.auth.exception;

import org.springframework.http.HttpStatus;
import lombok.Getter;

@Getter
public class AuthException extends RuntimeException {

    private final HttpStatus status;

    public AuthException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public static AuthException badRequest(String message) {
        return new AuthException(message, HttpStatus.BAD_REQUEST);
    }

    public static AuthException unauthorized(String message) {
        return new AuthException(message, HttpStatus.UNAUTHORIZED);
    }

    public static AuthException conflict(String message) {
        return new AuthException(message, HttpStatus.CONFLICT);
    }

    public static AuthException notFound(String message) {
        return new AuthException(message, HttpStatus.NOT_FOUND);
    }
}