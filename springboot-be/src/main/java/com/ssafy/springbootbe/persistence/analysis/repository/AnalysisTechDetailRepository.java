package com.ssafy.springbootbe.persistence.analysis.repository;

import com.ssafy.springbootbe.persistence.analysis.entity.AnalysisTechDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnalysisTechDetailRepository extends JpaRepository<AnalysisTechDetail, Long> {

    // 리포트 기준 기술 상세 조회 (idx_analysis_tech_detail_report)
    List<AnalysisTechDetail> findByAnalysisReportAnalysisReportId(Long analysisReportId);
}
