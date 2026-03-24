package com.ssafy.springbootbe.domain.activities.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ActivityInclusionResponse {

    private Long activityHistoryId;
    private Boolean isIncluded;
}
