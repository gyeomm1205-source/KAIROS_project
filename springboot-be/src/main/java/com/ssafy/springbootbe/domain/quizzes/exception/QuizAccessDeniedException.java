package com.ssafy.springbootbe.domain.quizzes.exception;

public class QuizAccessDeniedException extends RuntimeException {
    public QuizAccessDeniedException(Long curriculumId) {
        super("해당 퀴즈 대상 커리큘럼에 접근 권한이 없습니다. curriculumId=" + curriculumId);
    }
}
