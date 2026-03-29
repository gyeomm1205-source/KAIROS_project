package com.ssafy.springbootbe.domain.activities.dto.response;

import com.ssafy.springbootbe.persistence.activity.type.ActivityType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@Builder
@AllArgsConstructor
public class GrowthReportResponse {

    private List<TechStackCountInfo> topTechStacks;
    private Long totalActivityCount;
    private TechStackInfo recentGrowthTech;
    private Integer maxStreakDays;
    private List<TechStackScoreInfo> techScoreSnapshot;
    private List<MonthlyActivityCountInfo> monthlyActivityCounts;
    private List<TechStackCountInfo> techActivityRanking;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class TechStackCountInfo {
        private Long techStackId;
        private String techName;
        private String iconUrl;
        private String color;
        private Long count;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class TechStackInfo {
        private Long techStackId;
        private String techName;
        private String iconUrl;
        private String color;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class TechStackScoreInfo {
        private Long techStackId;
        private String techName;
        private String iconUrl;
        private String color;
        private Double score;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class MonthlyActivityCountInfo {
        private Integer year;
        private Integer month;
        private Map<ActivityType, Long> counts;
    }
}
