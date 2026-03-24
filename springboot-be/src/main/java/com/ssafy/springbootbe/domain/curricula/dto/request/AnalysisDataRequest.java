package com.ssafy.springbootbe.domain.curricula.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisDataRequest {

    private String summary;
    private List<TechDetailRequest> techDetails;
    private List<RecommendedPositionRequest> recommendedPositions;
}
