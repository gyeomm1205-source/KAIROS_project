package com.ssafy.springbootbe.domain.quizzes.exception;

public class QuizQuestionNotFoundException extends RuntimeException {
    public QuizQuestionNotFoundException(Long curriculumId, Integer questionNumber) {
        super("퀴즈 문제를 찾을 수 없습니다. curriculumId=" + curriculumId + ", questionNumber=" + questionNumber);
    }
}
