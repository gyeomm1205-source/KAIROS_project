package com.ssafy.springbootbe.domain.schedules.exception;

public class ScheduleAccessDeniedException extends RuntimeException {

    public ScheduleAccessDeniedException(Long scheduleId) {
        super("해당 일정에 접근 권한이 없습니다. scheduleId=" + scheduleId);
    }
}
