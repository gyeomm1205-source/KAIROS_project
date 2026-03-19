package com.ssafy.springbootbe.domain.calendar.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
@AllArgsConstructor
public class CalendarPeriod {

    private LocalDate start;
    private LocalDate end;
}
