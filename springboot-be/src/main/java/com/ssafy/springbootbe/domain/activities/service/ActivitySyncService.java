package com.ssafy.springbootbe.domain.activities.service;

import com.ssafy.springbootbe.domain.activities.dto.response.ActivitySyncResponse;

public interface ActivitySyncService {

    ActivitySyncResponse syncActivities(Long userId, String provider);
}