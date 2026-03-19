package com.ssafy.springbootbe.domain.activities.exception;

public class ActivityAccessDeniedException extends RuntimeException {

    public ActivityAccessDeniedException(Long activityHistoryId) {
        super("해당 활동 이력에 접근 권한이 없습니다. id=" + activityHistoryId);
    }
}
