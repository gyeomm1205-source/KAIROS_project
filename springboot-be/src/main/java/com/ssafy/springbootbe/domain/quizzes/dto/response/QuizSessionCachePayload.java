package com.ssafy.springbootbe.domain.quizzes.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizSessionCachePayload {
    private Long curriculumId;
    private Integer totalQuestions;
    private String title;
    private String description;
    private Integer expectedMinutes;
    private List<Question> questions;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Question {
        private Integer questionNumber;
        private String question;
        private String quizType;
        private List<String> options;
        private String correctAnswer;
        private String selectedAnswer;
        private Boolean isCorrect;
    }
}
