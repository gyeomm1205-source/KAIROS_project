package com.ssafy.springbootbe.domain.onboarding.exception;

public class OnboardingReferenceNotFoundException extends RuntimeException {

    public OnboardingReferenceNotFoundException(String message) {
        super(message);
    }
}
