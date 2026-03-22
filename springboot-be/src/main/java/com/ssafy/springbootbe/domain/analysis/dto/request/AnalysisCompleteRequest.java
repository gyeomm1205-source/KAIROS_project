package com.ssafy.springbootbe.domain.analysis.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor
public class AnalysisCompleteRequest {

    private Long userId;
    private String taskId;
    private List<TechScoreItem> techScores;
    private List<ActivityItem> githubActivities;
    private List<ActivityItem> velogActivities;

    @Getter
    @NoArgsConstructor
    public static class TechScoreItem {
        private String techName;
        private Integer score;
    }

    @Getter
    @NoArgsConstructor
    public static class ActivityItem {
        private String activityType;
        private String title;
        private String description;
        private LocalDateTime activityDate;
        private List<String> techStacks;
    }
}