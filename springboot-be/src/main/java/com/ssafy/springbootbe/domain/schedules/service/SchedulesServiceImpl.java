package com.ssafy.springbootbe.domain.schedules.service;

import com.ssafy.springbootbe.common.redis.RedisService;
import com.ssafy.springbootbe.common.utils.OAuthTokenCryptoService;
import com.ssafy.springbootbe.domain.calendar.service.GoogleCalendarClientService;
import com.ssafy.springbootbe.domain.schedules.dto.request.ScheduleCreateRequest;
import com.ssafy.springbootbe.domain.schedules.dto.request.ScheduleUpdateRequest;
import com.ssafy.springbootbe.domain.schedules.dto.response.ScheduleResponse;
import com.ssafy.springbootbe.domain.schedules.exception.ScheduleAccessDeniedException;
import com.ssafy.springbootbe.domain.schedules.exception.ScheduleNotFoundException;
import com.ssafy.springbootbe.persistence.oauth.entity.OAuthAccount;
import com.ssafy.springbootbe.persistence.oauth.repository.OAuthAccountRepository;
import com.ssafy.springbootbe.persistence.oauth.type.OAuthProvider;
import com.ssafy.springbootbe.persistence.schedule.entity.UserSchedule;
import com.ssafy.springbootbe.persistence.schedule.repository.UserScheduleRepository;
import com.ssafy.springbootbe.persistence.user.entity.User;
import com.ssafy.springbootbe.persistence.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDate;
import java.time.YearMonth;

@Slf4j
@Service
@RequiredArgsConstructor
public class SchedulesServiceImpl implements SchedulesService {

    private final UserScheduleRepository userScheduleRepository;
    private final UserRepository userRepository;
    private final RedisService redisService;
    private final OAuthAccountRepository oAuthAccountRepository;
    private final GoogleCalendarClientService googleCalendarClientService;
    private final OAuthTokenCryptoService oAuthTokenCryptoService;

    @Override
    @Transactional
    public ScheduleResponse createSchedule(Long userId, ScheduleCreateRequest request) {
        validateDateRange(request.getStartDate(), request.getEndDate());
        User user = findUserByIdOrThrow(userId);

        UserSchedule schedule = UserSchedule.builder()
                .user(user)
                .title(request.getTitle())
                .description(request.getDescription())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .build();

        UserSchedule savedSchedule = userScheduleRepository.save(schedule);
        invalidateCalendarCache(userId, request.getStartDate(), request.getEndDate());

        log.info("개인 일정 생성 완료. userId={}, scheduleId={}", userId, savedSchedule.getUserScheduleId());
        return ScheduleResponse.from(savedSchedule);
    }

    @Override
    @Transactional
    public ScheduleResponse updateSchedule(Long userId, Long scheduleId, ScheduleUpdateRequest request) {
        UserSchedule schedule = findScheduleByIdOrThrow(scheduleId);
        validateOwnership(schedule, userId);

        LocalDate effectiveStart = request.getStartDate() != null ? request.getStartDate() : schedule.getStartDate();
        LocalDate effectiveEnd = request.getEndDate() != null ? request.getEndDate() : schedule.getEndDate();
        validateDateRange(effectiveStart, effectiveEnd);

        LocalDate previousStartDate = schedule.getStartDate();
        LocalDate previousEndDate = schedule.getEndDate();

        schedule.update(request.getTitle(), request.getDescription(), request.getStartDate(), request.getEndDate());

        // 변경 전후 날짜 범위 모두 무효화
        invalidateCalendarCache(userId, previousStartDate, previousEndDate);
        if (request.getStartDate() != null || request.getEndDate() != null) {
            LocalDate newStartDate = request.getStartDate() != null ? request.getStartDate() : previousStartDate;
            LocalDate newEndDate = request.getEndDate() != null ? request.getEndDate() : previousEndDate;
            invalidateCalendarCache(userId, newStartDate, newEndDate);
        }

        log.info("개인 일정 수정 완료. userId={}, scheduleId={}", userId, scheduleId);
        return ScheduleResponse.from(schedule);
    }

    @Override
    @Transactional
    public void deleteSchedule(Long userId, Long scheduleId) {
        UserSchedule schedule = findScheduleByIdOrThrow(scheduleId);
        validateOwnership(schedule, userId);

        LocalDate startDate = schedule.getStartDate();
        LocalDate endDate = schedule.getEndDate();

        String googleEventId = schedule.getGoogleEventId();
        userScheduleRepository.delete(schedule);
        invalidateCalendarCache(userId, startDate, endDate);

        if (googleEventId != null) {
            deleteGoogleCalendarEvent(userId, googleEventId);
        }

        log.info("개인 일정 삭제 완료. userId={}, scheduleId={}", userId, scheduleId);
    }

    private User findUserByIdOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다. userId=" + userId));
    }

    private UserSchedule findScheduleByIdOrThrow(Long scheduleId) {
        return userScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ScheduleNotFoundException(scheduleId));
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("종료일은 시작일보다 앞설 수 없습니다.");
        }
    }

    private void validateOwnership(UserSchedule schedule, Long userId) {
        if (!schedule.getUser().getUserId().equals(userId)) {
            throw new ScheduleAccessDeniedException(schedule.getUserScheduleId());
        }
    }

    private void deleteGoogleCalendarEvent(Long userId, String googleEventId) {
        oAuthAccountRepository.findByUserUserIdAndProvider(userId, OAuthProvider.GOOGLE)
                .ifPresentOrElse(oAuthAccount -> {
                    try {
                        String refreshToken = oAuthTokenCryptoService.decrypt(oAuthAccount.getRefreshToken());
                        com.google.api.services.calendar.Calendar client =
                                googleCalendarClientService.buildCalendarClient(refreshToken);
                        googleCalendarClientService.deleteEvent(client, googleEventId);
                        log.info("Google Calendar 이벤트 삭제 완료. userId={}, eventId={}", userId, googleEventId);
                    } catch (IOException e) {
                        log.warn("Google Calendar 이벤트 삭제 실패. userId={}, eventId={}", userId, googleEventId, e);
                    }
                }, () -> log.warn("Google OAuth 계정 없음 — Google Calendar 이벤트 삭제 건너뜀. userId={}", userId));
    }

    private void invalidateCalendarCache(Long userId, LocalDate startDate, LocalDate endDate) {
        YearMonth start = YearMonth.from(startDate);
        YearMonth end = YearMonth.from(endDate);

        YearMonth current = start;
        while (!current.isAfter(end)) {
            String cacheKey = "calendar:" + userId + ":" + current.getYear() + ":" + current.getMonthValue();
            redisService.delete(cacheKey);
            current = current.plusMonths(1);
        }
    }
}
