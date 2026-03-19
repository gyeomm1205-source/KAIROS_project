package com.ssafy.springbootbe.domain.auth.exception;

public class InvalidOnboardingTokenException extends RuntimeException {

    public InvalidOnboardingTokenException(String message) {
        super(message);
    }

    public InvalidOnboardingTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}
