package com.ssafy.springbootbe.domain.recommendations.exception;

public class RecommendationAccessDeniedException extends RuntimeException {

    public RecommendationAccessDeniedException(Long curriculumId) {
        super("해당 추천 상세에 접근 권한이 없습니다. curriculumId=" + curriculumId);
    }
}
