package com.ssafy.springbootbe.domain.analysis.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class AnalysisCompleteResponse {

    private String message;
    private Integer savedCount;
}