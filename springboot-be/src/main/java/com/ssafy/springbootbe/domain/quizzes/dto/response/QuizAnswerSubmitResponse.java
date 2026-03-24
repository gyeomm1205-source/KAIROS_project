package com.ssafy.springbootbe.domain.quizzes.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class QuizAnswerSubmitResponse {
    private Integer questionNumber;
    private Boolean isCorrect;
    private String correctAnswer;
    private String selectedAnswer;
}
