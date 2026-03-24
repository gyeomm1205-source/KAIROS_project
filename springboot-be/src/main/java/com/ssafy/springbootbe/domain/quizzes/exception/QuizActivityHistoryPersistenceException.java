package com.ssafy.springbootbe.domain.quizzes.exception;

public class QuizActivityHistoryPersistenceException extends RuntimeException {
    public QuizActivityHistoryPersistenceException(String message, Throwable cause) {
        super(message, cause);
    }
}
