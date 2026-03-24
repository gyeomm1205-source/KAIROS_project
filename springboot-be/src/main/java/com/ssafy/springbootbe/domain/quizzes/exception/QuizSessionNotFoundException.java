package com.ssafy.springbootbe.domain.quizzes.exception;

public class QuizSessionNotFoundException extends RuntimeException {
    public QuizSessionNotFoundException(Long curriculumId) {
        super("진행 중인 퀴즈 세션을 찾을 수 없습니다. curriculumId=" + curriculumId);
    }
}
