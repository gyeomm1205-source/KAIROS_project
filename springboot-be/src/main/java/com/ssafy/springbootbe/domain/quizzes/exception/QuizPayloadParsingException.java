package com.ssafy.springbootbe.domain.quizzes.exception;

public class QuizPayloadParsingException extends RuntimeException {
    public QuizPayloadParsingException(String message, Throwable cause) {
        super(message, cause);
    }
}
