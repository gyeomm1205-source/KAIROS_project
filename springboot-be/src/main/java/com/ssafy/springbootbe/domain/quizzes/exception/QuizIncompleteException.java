package com.ssafy.springbootbe.domain.quizzes.exception;

public class QuizIncompleteException extends RuntimeException {
    public QuizIncompleteException(Long curriculumId) {
        super("아직 제출되지 않은 문제가 남아 있습니다. curriculumId=" + curriculumId);
    }
}
