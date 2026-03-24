package com.ssafy.springbootbe.domain.auth.exception;

public class GoogleUserInfoFetchFailedException extends RuntimeException {

    public GoogleUserInfoFetchFailedException(String message) {
        super(message);
    }

    public GoogleUserInfoFetchFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
