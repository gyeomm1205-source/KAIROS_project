package com.ssafy.springbootbe.domain.calendar.dto.response;

import com.ssafy.springbootbe.persistence.curriculum.type.CurriculumStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class CalendarCurriculumResponse {

    private Long curriculumId;
    private CurriculumStatus status;
    private LocalDate startDate;
    private LocalDate endDate;
    private List<CalendarNodeResponse> nodes;
}
