package com.ssafy.springbootbe.domain.quizzes.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class QuizSessionCompleteResponse {
    private Long curriculumId;
    private Integer totalScore;
    private List<Result> results;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class Result {
        private Integer questionNumber;
        private String question;
        private List<String> options;
        private String correctAnswer;
        private String selectedAnswer;
        private Boolean isCorrect;
    }
}
