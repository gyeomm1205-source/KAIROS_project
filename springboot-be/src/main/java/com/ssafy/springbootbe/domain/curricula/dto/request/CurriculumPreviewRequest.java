package com.ssafy.springbootbe.domain.curricula.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CurriculumPreviewRequest {

    @NotNull
    @Valid
    private AnalysisDataRequest analysisData;
}