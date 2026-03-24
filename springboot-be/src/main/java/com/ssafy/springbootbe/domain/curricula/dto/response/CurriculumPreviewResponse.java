package com.ssafy.springbootbe.domain.curricula.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CurriculumPreviewResponse {

    private String curriculumPreviewKey;
    private Integer duration;
    private PreviewReasonDto recommendationReason;
    private List<PreviewNodeDto> nodes;
}