package com.ssafy.springbootbe.persistence.analysis.repository;

import com.ssafy.springbootbe.persistence.analysis.entity.AnalysisReport;
import com.ssafy.springbootbe.persistence.analysis.type.AnalysisStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface AnalysisReportRepository extends JpaRepository<AnalysisReport, Long> {
    // 유저의 분석 리포트 목록 (idx_analysis_user)
    List<AnalysisReport> findByUserUserIdOrderByCreatedAtDesc(Long userId);
    // 진행 중인 분석 여부 확인 (409 중복 방지)
    boolean existsByUserUserIdAndStatusIn(Long userId, List<AnalysisStatus> statuses);
}
