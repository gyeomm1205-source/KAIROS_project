package com.ssafy.springbootbe.persistence.analysis.repository;

import com.ssafy.springbootbe.persistence.analysis.entity.AnalysisRecommendedPosition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnalysisRecommendedPositionRepository extends JpaRepository<AnalysisRecommendedPosition, Long> {

    List<AnalysisRecommendedPosition> findByAnalysisReportAnalysisReportId(Long analysisReportId);
}
