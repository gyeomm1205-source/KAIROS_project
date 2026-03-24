package com.ssafy.springbootbe.domain.quizzes.exception;

public class QuizRedisException extends RuntimeException {
    public QuizRedisException(String message, Throwable cause) {
        super(message, cause);
    }
}
