package com.ssafy.springbootbe.domain.quizzes.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;

import java.util.List;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class QuizGenerateAsyncResponse {
    private Long curriculumId;
    private Integer totalQuestions;
    private String title;
    private String description;
    private Integer expectedMinutes;
    private List<Question> questions;

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Question {
        private Integer questionNumber;
        private String question;
        private String quizType;
        private List<String> options;
        private String correctAnswer;
    }
}
