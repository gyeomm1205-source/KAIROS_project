package com.ssafy.springbootbe.domain.auth.exception;

public class AuthPersistenceException extends RuntimeException {

    public AuthPersistenceException(String message, Throwable cause) {
        super(message, cause);
    }
}
