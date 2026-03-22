package com.ssafy.springbootbe.domain.curricula.dto.response;

import com.ssafy.springbootbe.persistence.curriculum.entity.CurriculumNode;
import com.ssafy.springbootbe.persistence.curriculum.type.ProgressStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
@AllArgsConstructor
public class CurriculumNodeResponse {

    private Long curriculumNodeId;
    private Long curriculumId;
    private String title;
    private String description;
    private LocalDate scheduledDate;
    private Integer expectedMinutes;
    private ProgressStatus progressStatus;

    public static CurriculumNodeResponse from(CurriculumNode node) {
        return CurriculumNodeResponse.builder()
                .curriculumNodeId(node.getCurriculumNodeId())
                .curriculumId(node.getCurriculum().getCurriculumId())
                .title(node.getTitle())
                .description(node.getDescription())
                .scheduledDate(node.getScheduledDate())
                .expectedMinutes(node.getExpectedMinutes())
                .progressStatus(node.getProgressStatus())
                .build();
    }
}
