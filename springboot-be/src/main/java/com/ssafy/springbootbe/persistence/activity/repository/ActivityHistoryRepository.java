package com.ssafy.springbootbe.persistence.activity.repository;

import com.ssafy.springbootbe.persistence.activity.entity.ActivityHistory;
import com.ssafy.springbootbe.persistence.activity.type.ActivityType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ActivityHistoryRepository extends JpaRepository<ActivityHistory, Long> {

    List<ActivityHistory> findTop10ByUserUserIdAndIsIncludedTrueOrderByActivityDateDesc(Long userId);

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

    Optional<ActivityHistory> findTopByUserUserIdAndActivityTypeOrderByActivityDateDesc(Long userId, ActivityType activityType);

    // 성장 일지 — 월별 activity_type별 활동 수 집계
    @Query("SELECT YEAR(a.activityDate), MONTH(a.activityDate), a.activityType, COUNT(a) " +
           "FROM ActivityHistory a WHERE a.user.userId = :userId " +
           "GROUP BY YEAR(a.activityDate), MONTH(a.activityDate), a.activityType " +
           "ORDER BY YEAR(a.activityDate) ASC, MONTH(a.activityDate) ASC")
    List<Object[]> findMonthlyActivityCountsByUserId(@Param("userId") Long userId);

    // 동기화 — 특정 activityType의 최신 activityDate 조회 (마지막 동기화 시점)
    @Query("SELECT MAX(a.activityDate) FROM ActivityHistory a WHERE a.user.userId = :userId AND a.activityType = :activityType")
    Optional<LocalDateTime> findLatestActivityDateByUserIdAndActivityType(
            @Param("userId") Long userId,
            @Param("activityType") ActivityType activityType);

    // 동기화 — 최근 N일 특정 activityType 건수 조회
    @Query("SELECT COUNT(a) FROM ActivityHistory a WHERE a.user.userId = :userId AND a.activityType = :activityType AND a.activityDate >= :since")
    long countByUserIdAndActivityTypeSince(
            @Param("userId") Long userId,
            @Param("activityType") ActivityType activityType,
            @Param("since") LocalDateTime since);

    // 동기화 — 특정 activityType 전체 건수 조회
    @Query("SELECT COUNT(a) FROM ActivityHistory a WHERE a.user.userId = :userId AND a.activityType = :activityType")
    long countByUserIdAndActivityType(
            @Param("userId") Long userId,
            @Param("activityType") ActivityType activityType);
}
