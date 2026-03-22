package com.ssafy.springbootbe.domain.curricula.dto.response;

import com.ssafy.springbootbe.persistence.curriculum.entity.CurriculumRecommendationReason;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class CurriculumReasonResponse {

    private Long curriculumId;
    private String summaryLine;
    private String userContext;
    private String aiInterpretation;
    private String curriculumRationale;
    private LocalDateTime createdAt;

    public static CurriculumReasonResponse from(CurriculumRecommendationReason reason) {
        return CurriculumReasonResponse.builder()
                .curriculumId(reason.getCurriculum().getCurriculumId())
                .summaryLine(reason.getSummaryLine())
                .userContext(reason.getUserContext())
                .aiInterpretation(reason.getAiInterpretation())
                .curriculumRationale(reason.getCurriculumRationale())
                .createdAt(reason.getCreatedAt())
                .build();
    }
}