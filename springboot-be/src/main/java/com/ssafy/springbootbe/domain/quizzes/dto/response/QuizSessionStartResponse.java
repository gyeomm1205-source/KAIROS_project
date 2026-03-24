package com.ssafy.springbootbe.domain.quizzes.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class QuizSessionStartResponse {
    private Long curriculumId;
    private Integer totalQuestions;
    private String title;
    private String description;
    private Integer expectedMinutes;
    private List<Question> questions;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class Question {
        private Integer questionNumber;
        private String question;
        private String quizType;
        private List<String> options;
    }
}
