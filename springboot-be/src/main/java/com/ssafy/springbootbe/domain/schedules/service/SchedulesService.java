package com.ssafy.springbootbe.domain.schedules.service;

import com.ssafy.springbootbe.domain.schedules.dto.request.ScheduleCreateRequest;
import com.ssafy.springbootbe.domain.schedules.dto.request.ScheduleUpdateRequest;
import com.ssafy.springbootbe.domain.schedules.dto.response.ScheduleResponse;

public interface SchedulesService {

    ScheduleResponse createSchedule(Long userId, ScheduleCreateRequest request);

    ScheduleResponse updateSchedule(Long userId, Long scheduleId, ScheduleUpdateRequest request);

    void deleteSchedule(Long userId, Long scheduleId);
}
