package com.ssafy.springbootbe.domain.quizzes.exception;

public class QuizAlreadyCompletedException extends RuntimeException {
    public QuizAlreadyCompletedException(Long curriculumId) {
        super("이미 완료된 퀴즈입니다. curriculumId=" + curriculumId);
    }
}
