package com.ssafy.springbootbe.domain.schedules.dto.response;

import com.ssafy.springbootbe.persistence.schedule.entity.UserSchedule;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
@AllArgsConstructor
public class ScheduleResponse {

    private Long scheduleId;
    private String title;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
    private String googleEventId;

    public static ScheduleResponse from(UserSchedule schedule) {
        return ScheduleResponse.builder()
                .scheduleId(schedule.getUserScheduleId())
                .title(schedule.getTitle())
                .description(schedule.getDescription())
                .startDate(schedule.getStartDate())
                .endDate(schedule.getEndDate())
                .googleEventId(schedule.getGoogleEventId())
                .build();
    }
}
