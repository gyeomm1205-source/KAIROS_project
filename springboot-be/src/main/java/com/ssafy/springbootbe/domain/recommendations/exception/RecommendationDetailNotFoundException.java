package com.ssafy.springbootbe.domain.recommendations.exception;

public class RecommendationDetailNotFoundException extends RuntimeException {

    public RecommendationDetailNotFoundException(Long curriculumId) {
        super("추천 상세 Redis 데이터를 찾을 수 없습니다. curriculumId=" + curriculumId);
    }
}
