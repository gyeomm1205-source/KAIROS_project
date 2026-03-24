package com.ssafy.springbootbe.domain.recommendations.exception;

public class RecommendationRedisLookupException extends RuntimeException {

    public RecommendationRedisLookupException(Long userId, Long curriculumId, Throwable cause) {
        super("추천 Redis 조회에 실패했습니다. userId=" + userId + ", curriculumId=" + curriculumId, cause);
    }
}
