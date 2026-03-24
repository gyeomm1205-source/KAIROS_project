package com.ssafy.springbootbe.domain.recommendations.exception;

public class RecommendationCurriculumNotFoundException extends RuntimeException {

    public RecommendationCurriculumNotFoundException(Long curriculumId) {
        super("추천 상세 대상 커리큘럼을 찾을 수 없습니다. curriculumId=" + curriculumId);
    }
}
