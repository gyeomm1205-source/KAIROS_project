package com.ssafy.springbootbe.domain.schedules.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleUpdateRequest {

    private String title;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
}
