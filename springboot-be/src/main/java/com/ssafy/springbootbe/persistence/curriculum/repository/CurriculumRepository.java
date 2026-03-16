package com.ssafy.springbootbe.persistence.curriculum.repository;

import com.ssafy.springbootbe.persistence.curriculum.entity.Curriculum;
import com.ssafy.springbootbe.persistence.curriculum.type.CurriculumStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface CurriculumRepository extends JpaRepository<Curriculum, Long> {
    // 유저의 전체 커리큘럼 조회 (idx_curriculum_user)
    List<Curriculum> findByUserUserIdOrderByCreatedAtDesc(Long userId);
    // 유저의 특정 상태 커리큘럼 조회 (추천 페이지 목록)
    List<Curriculum> findByUserUserIdAndStatus(Long userId, CurriculumStatus status);
    // ACTIVE 커리큘럼 중복 확인 (409 방지)
    boolean existsByUserUserIdAndStatus(Long userId, CurriculumStatus status);
}
