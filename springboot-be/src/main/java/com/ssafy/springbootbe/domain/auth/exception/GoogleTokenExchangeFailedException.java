package com.ssafy.springbootbe.domain.auth.exception;

public class GoogleTokenExchangeFailedException extends RuntimeException {

    public GoogleTokenExchangeFailedException(String message) {
        super(message);
    }

    public GoogleTokenExchangeFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
