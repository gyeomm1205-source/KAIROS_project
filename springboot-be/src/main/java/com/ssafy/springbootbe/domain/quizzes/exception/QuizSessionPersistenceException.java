package com.ssafy.springbootbe.domain.quizzes.exception;

public class QuizSessionPersistenceException extends RuntimeException {
    public QuizSessionPersistenceException(String message, Throwable cause) {
        super(message, cause);
    }
}
