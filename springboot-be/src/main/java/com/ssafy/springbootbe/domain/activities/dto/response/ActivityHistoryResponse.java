package com.ssafy.springbootbe.domain.activities.dto.response;

import com.ssafy.springbootbe.common.dto.TechStackInfo;
import com.ssafy.springbootbe.persistence.activity.entity.ActivityHistory;
import com.ssafy.springbootbe.persistence.activity.type.ActivityType;
import com.ssafy.springbootbe.persistence.user.type.CurriculumCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class ActivityHistoryResponse {

    private Long activityHistoryId;
    private ActivityType activityType;
    private CurriculumCategory category;
    private String title;
    private String description;
    private Boolean isIncluded;
    private LocalDateTime activityDate;
    private List<TechStackInfo> techStacks;

    public static ActivityHistoryResponse from(ActivityHistory activity, List<TechStackInfo> techStacks) {
        return ActivityHistoryResponse.builder()
                .activityHistoryId(activity.getActivityHistoryId())
                .activityType(activity.getActivityType())
                .category(activity.getCategory())
                .title(activity.getTitle())
                .description(activity.getDescription())
                .isIncluded(activity.getIsIncluded())
                .activityDate(activity.getActivityDate())
                .techStacks(techStacks)
                .build();
    }
}
