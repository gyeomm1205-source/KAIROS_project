package com.ssafy.springbootbe.domain.recommendations.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class RecommendationDetailResponse {
    private Long curriculumId;
    private RecommendationReason recommendationReason;
    private RecommendationCurrentStatus currentStatus;
    private List<RecommendationQuiz> quizzes;
    private List<RecommendationReference> references;
    private List<RecommendationNextNode> nextNodes;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class RecommendationReason {
        private String summary;
        private String detail;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class RecommendationCurrentStatus {
        private String summary;
        private String detail;
        private List<String> topSkills;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class RecommendationQuiz {
        private String title;
        private String description;
        private Integer expectedMinutes;
        private Integer totalQuestions;
        private List<RecommendationQuizQuestion> questions;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class RecommendationQuizQuestion {
        private Integer questionNumber;
        private String question;
        private String quizType;
        private List<String> options;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class RecommendationReference {
        private Long referenceId;
        private String title;
        private String recommendationReason;
        private String referenceType;
        private LocalDate publishedAt;
        private String url;
        private List<String> techStacks;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class RecommendationNextNode {
        private String title;
    }
}
