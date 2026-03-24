package com.ssafy.springbootbe.domain.recommendations.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationListResponse {

    private List<RecommendationListItemResponse> items;
}
