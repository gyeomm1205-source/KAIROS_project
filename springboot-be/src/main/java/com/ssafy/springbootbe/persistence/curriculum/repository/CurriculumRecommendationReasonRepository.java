package com.ssafy.springbootbe.persistence.curriculum.repository;

import com.ssafy.springbootbe.persistence.curriculum.entity.CurriculumRecommendationReason;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CurriculumRecommendationReasonRepository extends JpaRepository<CurriculumRecommendationReason, Long> {

    // 커리큘럼 AI 추천 근거 조회
    Optional<CurriculumRecommendationReason> findByCurriculumCurriculumId(Long curriculumId);
}
