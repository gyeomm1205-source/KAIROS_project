package com.ssafy.springbootbe.domain.recommendations.exception;

public class RecommendationCurriculumRetrievalException extends RuntimeException {

    public RecommendationCurriculumRetrievalException(Long userId, Throwable cause) {
        super("추천 목록용 curriculum 조회에 실패했습니다. userId=" + userId, cause);
    }
}
