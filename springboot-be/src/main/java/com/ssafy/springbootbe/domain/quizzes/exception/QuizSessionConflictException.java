package com.ssafy.springbootbe.domain.quizzes.exception;

public class QuizSessionConflictException extends RuntimeException {
    public QuizSessionConflictException(Long userId) {
        super("이미 진행 중인 퀴즈 세션이 존재합니다. userId=" + userId);
    }
}
