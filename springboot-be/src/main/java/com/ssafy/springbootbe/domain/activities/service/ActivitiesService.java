package com.ssafy.springbootbe.domain.activities.service;

import com.ssafy.springbootbe.domain.activities.dto.request.ActivityInclusionRequest;
import com.ssafy.springbootbe.domain.activities.dto.response.ActivityInclusionResponse;
import com.ssafy.springbootbe.domain.activities.dto.response.ActivityPageResponse;

public interface ActivitiesService {

    ActivityPageResponse findActivities(Long userId, Integer year, Integer month, String sort, int page, int size);

    ActivityInclusionResponse updateInclusion(Long userId, Long activityHistoryId, ActivityInclusionRequest request);
}
