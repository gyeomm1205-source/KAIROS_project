package com.ssafy.springbootbe.persistence.activity.repository;

import com.ssafy.springbootbe.persistence.activity.entity.ActivityHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ActivityHistoryRepository extends JpaRepository<ActivityHistory, Long> {

    // 유저 기준 페이지네이션 조회 (idx_activity_user)
    Page<ActivityHistory> findByUserUserId(Long userId, Pageable pageable);

    // 연도 필터 조회 (idx_activity_user_date)
    @Query("SELECT a FROM ActivityHistory a WHERE a.user.userId = :userId " +
           "AND YEAR(a.activityDate) = :year")
    Page<ActivityHistory> findByUserUserIdAndYear(
            @Param("userId") Long userId,
            @Param("year") int year,
            Pageable pageable);

    // 연도 + 월 필터 조회
    @Query("SELECT a FROM ActivityHistory a WHERE a.user.userId = :userId " +
           "AND YEAR(a.activityDate) = :year AND MONTH(a.activityDate) = :month")
    Page<ActivityHistory> findByUserUserIdAndYearAndMonth(
            @Param("userId") Long userId,
            @Param("year") int year,
            @Param("month") int month,
            Pageable pageable);

    // 성장 일지 — 전체 활동 수
    long countByUserUserId(Long userId);

    // 성장 일지 — 연속 활동일 계산용 전체 날짜 조회
    @Query("SELECT a.activityDate FROM ActivityHistory a WHERE a.user.userId = :userId ORDER BY a.activityDate ASC")
    List<LocalDateTime> findActivityDatesByUserId(@Param("userId") Long userId);

    // 성장 일지 — 월별 activity_type별 활동 수 집계
    @Query("SELECT YEAR(a.activityDate), MONTH(a.activityDate), a.activityType, COUNT(a) " +
           "FROM ActivityHistory a WHERE a.user.userId = :userId " +
           "GROUP BY YEAR(a.activityDate), MONTH(a.activityDate), a.activityType " +
           "ORDER BY YEAR(a.activityDate) ASC, MONTH(a.activityDate) ASC")
    List<Object[]> findMonthlyActivityCountsByUserId(@Param("userId") Long userId);
}
