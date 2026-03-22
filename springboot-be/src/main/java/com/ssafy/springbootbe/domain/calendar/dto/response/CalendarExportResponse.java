package com.ssafy.springbootbe.domain.calendar.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CalendarExportResponse {

    private int exportedNodes;
    private int exportedSchedules;
}