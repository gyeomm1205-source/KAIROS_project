package com.ssafy.springbootbe.persistence.schedule.repository;

import com.ssafy.springbootbe.persistence.schedule.entity.UserSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface UserScheduleRepository extends JpaRepository<UserSchedule, Long> {

    // 월별 캘린더 조회 — 기간 내 개인 일정 조회 (idx_user_schedule_user)
    List<UserSchedule> findByUserUserIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            Long userId, LocalDate endDate, LocalDate startDate);

    // Google Calendar import 시 eventId로 기존 일정 조회 (idx_user_schedule_google_event)
    Optional<UserSchedule> findByGoogleEventId(String googleEventId);

    // Google Calendar import 시 미연동 일정 조회
    List<UserSchedule> findByUserUserIdAndGoogleEventIdIsNull(Long userId);
}
