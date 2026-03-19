package com.ssafy.springbootbe.domain.auth.exception;

public class AuthTokenGenerationException extends RuntimeException {

    public AuthTokenGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
