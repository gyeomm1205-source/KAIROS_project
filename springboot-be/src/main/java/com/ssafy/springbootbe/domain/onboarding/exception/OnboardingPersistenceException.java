package com.ssafy.springbootbe.domain.onboarding.exception;

public class OnboardingPersistenceException extends RuntimeException {

    public OnboardingPersistenceException(String message, Throwable cause) {
        super(message, cause);
    }
}
