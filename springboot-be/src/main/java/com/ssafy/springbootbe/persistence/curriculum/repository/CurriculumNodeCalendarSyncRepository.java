package com.ssafy.springbootbe.persistence.curriculum.repository;

import com.ssafy.springbootbe.persistence.curriculum.entity.CurriculumNodeCalendarSync;
import com.ssafy.springbootbe.persistence.curriculum.type.SyncStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface CurriculumNodeCalendarSyncRepository extends JpaRepository<CurriculumNodeCalendarSync, Long> {
    Optional<CurriculumNodeCalendarSync> findByCurriculumNodeCurriculumNodeId(Long curriculumNodeId);
    Optional<CurriculumNodeCalendarSync> findByGoogleEventId(String googleEventId);
    // 내보내기 시 미연동 노드 조회
    List<CurriculumNodeCalendarSync> findByCurriculumNodeCurriculumUserUserIdAndSyncStatus(
            Long userId, SyncStatus syncStatus);
}
