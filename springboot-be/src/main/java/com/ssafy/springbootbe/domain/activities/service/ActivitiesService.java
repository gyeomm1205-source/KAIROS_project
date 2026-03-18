package com.ssafy.springbootbe.domain.activities.service;

import com.ssafy.springbootbe.domain.activities.dto.response.ActivityPageResponse;

public interface ActivitiesService {

    ActivityPageResponse findActivities(Long userId, Integer year, Integer month, String sort, int page, int size);
}
