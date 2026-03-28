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

    Page<ActivityHistory> findByUserUserId(Long userId, Pageable pageable);

    @Query("SELECT a FROM ActivityHistory a WHERE a.user.userId = :userId " +
           "AND YEAR(a.activityDate) = :year")
    Page<ActivityHistory> findByUserUserIdAndYear(
            @Param("userId") Long userId,
            @Param("year") int year,
            Pageable pageable);

    @Query("SELECT a FROM ActivityHistory a WHERE a.user.userId = :userId " +
           "AND YEAR(a.activityDate) = :year AND MONTH(a.activityDate) = :month")
    Page<ActivityHistory> findByUserUserIdAndYearAndMonth(
            @Param("userId") Long userId,
            @Param("year") int year,
            @Param("month") int month,
            Pageable pageable);

    long countByUserUserId(Long userId);

    @Query("SELECT a.activityDate FROM ActivityHistory a WHERE a.user.userId = :userId ORDER BY a.activityDate ASC")
    List<LocalDateTime> findActivityDatesByUserId(@Param("userId") Long userId);

    Optional<ActivityHistory> findTopByUserUserIdAndActivityTypeOrderByActivityDateDesc(Long userId, ActivityType activityType);

    Optional<ActivityHistory> findTopByUserUserIdAndActivityTypeAndTitleOrderByActivityDateDesc(
            Long userId,
            ActivityType activityType,
            String title);

    boolean existsByUserUserIdAndActivityTypeAndCurriculumIdAndActivityDateBetween(
            Long userId,
            ActivityType activityType,
            Long curriculumId,
            LocalDateTime start,
            LocalDateTime end);

    @Query("SELECT YEAR(a.activityDate), MONTH(a.activityDate), a.activityType, COUNT(a) " +
           "FROM ActivityHistory a WHERE a.user.userId = :userId " +
           "GROUP BY YEAR(a.activityDate), MONTH(a.activityDate), a.activityType " +
           "ORDER BY YEAR(a.activityDate) ASC, MONTH(a.activityDate) ASC")
    List<Object[]> findMonthlyActivityCountsByUserId(@Param("userId") Long userId);

    @Query("SELECT MAX(a.activityDate) FROM ActivityHistory a WHERE a.user.userId = :userId AND a.activityType = :activityType")
    Optional<LocalDateTime> findLatestActivityDateByUserIdAndActivityType(
            @Param("userId") Long userId,
            @Param("activityType") ActivityType activityType);

    @Query("SELECT COUNT(a) FROM ActivityHistory a WHERE a.user.userId = :userId AND a.activityType = :activityType AND a.activityDate >= :since")
    long countByUserIdAndActivityTypeSince(
            @Param("userId") Long userId,
            @Param("activityType") ActivityType activityType,
            @Param("since") LocalDateTime since);

    @Query("SELECT COUNT(a) FROM ActivityHistory a WHERE a.user.userId = :userId AND a.activityType = :activityType")
    long countByUserIdAndActivityType(
            @Param("userId") Long userId,
            @Param("activityType") ActivityType activityType);
}
