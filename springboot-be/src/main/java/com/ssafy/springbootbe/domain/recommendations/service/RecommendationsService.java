package com.ssafy.springbootbe.domain.recommendations.service;

import com.ssafy.springbootbe.domain.recommendations.dto.response.RecommendationDetailResponse;
import com.ssafy.springbootbe.domain.recommendations.dto.response.RecommendationListResponse;

public interface RecommendationsService {
    RecommendationListResponse findRecommendations(Long userId);
    RecommendationDetailResponse findRecommendationDetail(Long userId, Long curriculumId);
    void refreshDailyRecommendations();
    void refreshDailyRecommendation(Long userId, Long curriculumId);
}
