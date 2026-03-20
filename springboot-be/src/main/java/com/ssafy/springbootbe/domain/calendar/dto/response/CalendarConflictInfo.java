package com.ssafy.springbootbe.domain.calendar.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class CalendarConflictInfo {

    private Long curriculumNodeId;
    private String title;
    private LocalDate ourDate;
    private LocalDate googleDate;
    private List<String> options;
}