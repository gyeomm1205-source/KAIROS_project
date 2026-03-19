package com.ssafy.springbootbe.domain.calendar.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class CalendarResponse {

    private CalendarPeriod period;
    private List<CalendarCurriculumResponse> curricula;
    private List<CalendarScheduleResponse> personalSchedules;
}
