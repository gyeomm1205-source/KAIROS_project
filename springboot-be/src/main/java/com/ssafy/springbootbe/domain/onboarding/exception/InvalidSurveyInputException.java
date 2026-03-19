package com.ssafy.springbootbe.domain.onboarding.exception;

public class InvalidSurveyInputException extends RuntimeException {

    public InvalidSurveyInputException(String message) {
        super(message);
    }
}
