package com.ssafy.springbootbe.domain.recommendations.exception;

public class RecommendationNodeAggregationException extends RuntimeException {

    public RecommendationNodeAggregationException(Long userId, Throwable cause) {
        super("추천 목록용 curriculum_node 집계에 실패했습니다. userId=" + userId, cause);
    }
}
