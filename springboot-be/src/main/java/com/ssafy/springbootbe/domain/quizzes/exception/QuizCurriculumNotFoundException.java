package com.ssafy.springbootbe.domain.quizzes.exception;

public class QuizCurriculumNotFoundException extends RuntimeException {
    public QuizCurriculumNotFoundException(Long curriculumId) {
        super("퀴즈 대상 커리큘럼을 찾을 수 없습니다. curriculumId=" + curriculumId);
    }
}
