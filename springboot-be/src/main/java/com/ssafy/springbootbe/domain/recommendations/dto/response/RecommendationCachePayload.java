package com.ssafy.springbootbe.domain.recommendations.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class RecommendationCachePayload {
    private Long userId;
    private Long curriculumId;
    private RecommendationReason recommendationReason;
    private CurrentStatus currentStatus;
    private List<ReferenceItem> references;
    private List<NextNodeItem> nextNodes;

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RecommendationReason {
        private String summary;
        private String detail;
    }

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CurrentStatus {
        private String summary;
        private String detail;
        private List<String> topSkills;
    }

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ReferenceItem {
        private Long referenceId;
        private String title;
        private String recommendationReason;
        private String referenceType;
        private LocalDate publishedAt;
        private String url;
        private List<String> techStacks;
    }

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class NextNodeItem {
        private String title;
    }
}
