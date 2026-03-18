package com.ssafy.springbootbe.domain.activities.exception;

public class ActivityNotFoundException extends RuntimeException {

    public ActivityNotFoundException(Long activityHistoryId) {
        super("활동 이력을 찾을 수 없습니다. activityHistoryId=" + activityHistoryId);
    }
}
