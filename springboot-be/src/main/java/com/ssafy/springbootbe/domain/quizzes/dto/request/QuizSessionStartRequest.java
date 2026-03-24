package com.ssafy.springbootbe.domain.quizzes.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class QuizSessionStartRequest {

    @NotNull(message = "curriculumId는 필수입니다.")
    private Long curriculumId;
}
