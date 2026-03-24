package com.ssafy.springbootbe.domain.quizzes.exception;

public class QuizSourceNotFoundException extends RuntimeException {
    public QuizSourceNotFoundException(Long curriculumId) {
        super("퀴즈 원본 Redis 데이터를 찾을 수 없습니다. curriculumId=" + curriculumId);
    }
}
