package com.ssafy.springbootbe.domain.recommendations.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyRecommendationGenerateRequest {

    private Long userId;
    private Long curriculumId;
    private String currentLevel;
    private List<String> favoriteTechStacks;
    private List<SkillStat> skillStats;
    private List<RecentActivity> recentActivities;
    private List<GoogleCalendarEvent> googleCalendarEvents;
    private List<Long> recentCurriculaIds;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SkillStat {
        private String skill;
        private Long count;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentActivity {
        private String activityType;
        private String category;
        private String title;
        private String description;
        private LocalDateTime activityDate;
        private List<String> techStacks;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GoogleCalendarEvent {
        private String title;
        private LocalDate startDate;
        private LocalDate endDate;
    }
}
