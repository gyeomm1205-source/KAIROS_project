package com.ssafy.springbootbe.domain.recommendations.service;

import com.ssafy.springbootbe.domain.recommendations.dto.response.RecommendationListResponse;

public interface RecommendationsService {
    RecommendationListResponse findRecommendations(Long userId);
    void refreshDailyRecommendations();
}
