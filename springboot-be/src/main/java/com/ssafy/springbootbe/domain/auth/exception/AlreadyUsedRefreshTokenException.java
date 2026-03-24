package com.ssafy.springbootbe.domain.auth.exception;

public class AlreadyUsedRefreshTokenException extends RuntimeException {

    public AlreadyUsedRefreshTokenException(String message) {
        super(message);
    }
}
