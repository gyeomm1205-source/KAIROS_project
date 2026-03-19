package com.ssafy.springbootbe.domain.auth.exception;

public class AuthCookieProcessingException extends RuntimeException {

    public AuthCookieProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
