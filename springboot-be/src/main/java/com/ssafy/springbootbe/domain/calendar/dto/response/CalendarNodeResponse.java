package com.ssafy.springbootbe.domain.calendar.dto.response;

import com.ssafy.springbootbe.persistence.curriculum.entity.CurriculumNode;
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
public class CalendarNodeResponse {

    private Long curriculumNodeId;
    private String title;
    private String description;
    private LocalDate scheduledDate;
    private Integer expectedMinutes;
    private ProgressStatus progressStatus;

    public static CalendarNodeResponse from(CurriculumNode node) {
        return CalendarNodeResponse.builder()
                .curriculumNodeId(node.getCurriculumNodeId())
                .title(node.getTitle())
                .description(node.getDescription())
                .scheduledDate(node.getScheduledDate())
                .expectedMinutes(node.getExpectedMinutes())
                .progressStatus(node.getProgressStatus())
                .build();
    }
}
