package com.ssafy.springbootbe.domain.curricula.dto.response;

import com.ssafy.springbootbe.persistence.curriculum.type.ProgressStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmNodeDto {

    private Long curriculumNodeId;
    private String title;
    private LocalDate scheduledDate;
    private Integer expectedMinutes;
    private ProgressStatus progressStatus;
    private CalendarSyncDto calendarSync;
}