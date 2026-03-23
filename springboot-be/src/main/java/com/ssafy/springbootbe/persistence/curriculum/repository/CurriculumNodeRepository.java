package com.ssafy.springbootbe.persistence.curriculum.repository;

import com.ssafy.springbootbe.persistence.curriculum.entity.CurriculumNode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface CurriculumNodeRepository extends JpaRepository<CurriculumNode, Long> {

    // 커리큘럼 내 전체 노드 조회 (idx_curriculum_node_curriculum)
    List<CurriculumNode> findByCurriculumCurriculumIdOrderByScheduledDate(Long curriculumId);

    List<CurriculumNode> findByCurriculumCurriculumIdInOrderByCurriculumCurriculumIdAscScheduledDateAsc(
            List<Long> curriculumIds);

    // 월별 캘린더 조회 — 기간 내 노드 조회 (idx_curriculum_node_schedule)
    List<CurriculumNode> findByCurriculumCurriculumIdAndScheduledDateBetween(
            Long curriculumId, LocalDate startDate, LocalDate endDate);

    // 유저의 특정 기간 내 노드 조회 (캘린더 API)
    List<CurriculumNode> findByCurriculumUserUserIdAndScheduledDateBetween(
            Long userId, LocalDate startDate, LocalDate endDate);
}
