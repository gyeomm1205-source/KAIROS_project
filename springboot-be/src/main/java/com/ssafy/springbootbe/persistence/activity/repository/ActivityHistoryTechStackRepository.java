package com.ssafy.springbootbe.persistence.activity.repository;

import com.ssafy.springbootbe.persistence.activity.entity.ActivityHistoryTechStack;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ActivityHistoryTechStackRepository extends JpaRepository<ActivityHistoryTechStack, Long> {

    List<ActivityHistoryTechStack> findByActivityHistoryActivityHistoryId(Long activityHistoryId);

    void deleteByActivityHistoryActivityHistoryId(Long activityHistoryId);

    @Query("SELECT ahts.techStack, COUNT(ahts) as cnt FROM ActivityHistoryTechStack ahts " +
           "JOIN ahts.activityHistory ah WHERE ah.user.userId = :userId " +
           "AND ah.isIncluded = true " +
           "GROUP BY ahts.techStack ORDER BY cnt DESC")
    List<Object[]> findIncludedTechStackCountsByUserId(@Param("userId") Long userId);

    // 성장 일지 — 전체 기간 기술 스택 등장 횟수 (topTechStacks / techActivityRanking 공용)
    @Query("SELECT ahts.techStack, COUNT(ahts) as cnt FROM ActivityHistoryTechStack ahts " +
           "JOIN ahts.activityHistory ah WHERE ah.user.userId = :userId " +
           "GROUP BY ahts.techStack ORDER BY cnt DESC")
    List<Object[]> findTechStackCountsByUserId(@Param("userId") Long userId, Pageable pageable);

    // 성장 일지 — 최근 30일 기술 스택 등장 횟수 (recentGrowthTech)
    // TODO: activity_type별 가산점 확정 후 CASE 가중치 적용
    @Query("SELECT ahts.techStack, COUNT(ahts) as cnt FROM ActivityHistoryTechStack ahts " +
           "JOIN ahts.activityHistory ah WHERE ah.user.userId = :userId " +
           "AND ah.activityDate >= :since " +
           "GROUP BY ahts.techStack ORDER BY cnt DESC")
    List<Object[]> findTechStackCountsSince(@Param("userId") Long userId,
                                            @Param("since") LocalDateTime since,
                                            Pageable pageable);

    @Query("SELECT ahts FROM ActivityHistoryTechStack ahts " +
           "JOIN FETCH ahts.activityHistory ah " +
           "JOIN FETCH ahts.techStack ts " +
           "WHERE ah.user.userId = :userId AND ah.isIncluded = true")
    List<ActivityHistoryTechStack> findIncludedByUserId(@Param("userId") Long userId);
}
