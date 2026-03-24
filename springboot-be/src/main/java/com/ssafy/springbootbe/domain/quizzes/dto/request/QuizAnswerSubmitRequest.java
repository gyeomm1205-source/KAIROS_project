package com.ssafy.springbootbe.domain.quizzes.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class QuizAnswerSubmitRequest {

    @NotNull(message = "questionNumber는 필수입니다.")
    private Integer questionNumber;

    @NotBlank(message = "selectedAnswer는 필수입니다.")
    private String selectedAnswer;
}
