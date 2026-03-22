package com.ssafy.springbootbe.domain.calendar.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class CalendarImportResponse {

    private List<CalendarConflictInfo> conflictNodes;
    private int updatedSchedules;
    private int newSchedules;
}