package com.ssafy.springbootbe.domain.curricula.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class CurriculumGenerateRequest {

    private Long userId;
    private String curriculumType;
    private Boolean considerPersonalSchedule;
    private List<GoogleCalendarEventDto> googleCalendarEvents;
    private AnalysisDataRequest analysisData;
    private List<SkillStatDto> userTechStacks;
    private List<RecentActivityDto> recentActivities;
}
