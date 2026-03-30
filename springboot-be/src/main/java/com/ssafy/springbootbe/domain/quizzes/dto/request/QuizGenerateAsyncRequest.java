package com.ssafy.springbootbe.domain.quizzes.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class QuizGenerateAsyncRequest {
    private Long curriculumId;
    private List<String> targetTechStacks;
    private String userLevel;
    private String currentNodeTitle;
    private String currentNodeDescription;
    private LocalDate currentNodeDate;
}
