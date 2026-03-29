package com.ssafy.springbootbe.domain.curricula.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CurriculumPreviewRequest {

    private AnalysisDataRequest analysisData;
    private java.util.List<String> excludedTechStacks;
}
