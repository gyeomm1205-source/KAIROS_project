package com.ssafy.springbootbe.domain.quizzes.exception;

public class QuizAnswerConflictException extends RuntimeException {
    public QuizAnswerConflictException(Long curriculumId, Integer questionNumber) {
        super("이미 답안을 제출한 문제입니다. curriculumId=" + curriculumId + ", questionNumber=" + questionNumber);
    }
}
