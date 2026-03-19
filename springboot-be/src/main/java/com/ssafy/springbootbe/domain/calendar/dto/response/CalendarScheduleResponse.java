package com.ssafy.springbootbe.domain.calendar.dto.response;

import com.ssafy.springbootbe.persistence.schedule.entity.UserSchedule;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalendarScheduleResponse {

    private Long scheduleId;
    private String title;
    private LocalDate startDate;
    private LocalDate endDate;
    private String googleEventId;

    public static CalendarScheduleResponse from(UserSchedule schedule) {
        return CalendarScheduleResponse.builder()
                .scheduleId(schedule.getUserScheduleId())
                .title(schedule.getTitle())
                .startDate(schedule.getStartDate())
                .endDate(schedule.getEndDate())
                .googleEventId(schedule.getGoogleEventId())
                .build();
    }
}
