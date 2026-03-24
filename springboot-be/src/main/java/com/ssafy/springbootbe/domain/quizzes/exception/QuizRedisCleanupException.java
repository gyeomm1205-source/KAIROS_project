package com.ssafy.springbootbe.domain.quizzes.exception;

public class QuizRedisCleanupException extends RuntimeException {
    public QuizRedisCleanupException(String message) {
        super(message);
    }

    public QuizRedisCleanupException(String message, Throwable cause) {
        super(message, cause);
    }
}
