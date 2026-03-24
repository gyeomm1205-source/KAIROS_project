package com.ssafy.springbootbe.domain.calendar.service;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.ssafy.springbootbe.common.redis.RedisService;
import com.ssafy.springbootbe.common.utils.OAuthTokenCryptoService;
import com.ssafy.springbootbe.domain.calendar.dto.response.CalendarConflictInfo;
import com.ssafy.springbootbe.domain.calendar.dto.response.CalendarCurriculumResponse;
import com.ssafy.springbootbe.domain.calendar.dto.response.CalendarExportResponse;
import com.ssafy.springbootbe.domain.calendar.dto.response.CalendarImportResponse;
import com.ssafy.springbootbe.domain.calendar.dto.response.CalendarNodeResponse;
import com.ssafy.springbootbe.domain.calendar.dto.response.CalendarPeriod;
import com.ssafy.springbootbe.domain.calendar.dto.response.CalendarResponse;
import com.ssafy.springbootbe.domain.calendar.dto.response.CalendarScheduleResponse;
import com.ssafy.springbootbe.domain.calendar.exception.GoogleOAuthNotFoundException;
import com.ssafy.springbootbe.persistence.curriculum.entity.Curriculum;
import com.ssafy.springbootbe.persistence.curriculum.entity.CurriculumNode;
import com.ssafy.springbootbe.persistence.curriculum.entity.CurriculumNodeCalendarSync;
import com.ssafy.springbootbe.persistence.curriculum.repository.CurriculumNodeCalendarSyncRepository;
import com.ssafy.springbootbe.persistence.curriculum.repository.CurriculumNodeRepository;
import com.ssafy.springbootbe.persistence.curriculum.type.SyncStatus;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


@Slf4j
@Service
@RequiredArgsConstructor
public class CalendarServiceImpl implements CalendarService {

    private final CurriculumNodeRepository curriculumNodeRepository;
    private final UserScheduleRepository userScheduleRepository;
    private final CurriculumNodeCalendarSyncRepository curriculumNodeCalendarSyncRepository;
    private final OAuthAccountRepository oAuthAccountRepository;
    private final UserRepository userRepository;
    private final RedisService redisService;
    private final ObjectMapper objectMapper;
    private final GoogleCalendarClientService googleCalendarClientService;
    private final OAuthTokenCryptoService oAuthTokenCryptoService;

    @Override
    @Transactional(readOnly = true)
    public CalendarResponse getCalendar(Long userId, int year, int month) {
        String cacheKey = "calendar:" + userId + ":" + year + ":" + month;
        String cached = redisService.get(cacheKey);
        if (cached != null) {
            try {
                return objectMapper.readValue(cached, CalendarResponse.class);
            } catch (JacksonException e) {
                log.warn("캘린더 캐시 역직렬화 실패. key={}", cacheKey, e);
            }
        }

        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        List<CalendarCurriculumResponse> curricula = buildCurriculaResponse(userId, startDate, endDate);
        List<CalendarScheduleResponse> personalSchedules = buildScheduleResponse(userId, startDate, endDate);

        CalendarResponse response = CalendarResponse.builder()
                .period(CalendarPeriod.builder().start(startDate).end(endDate).build())
                .curricula(curricula)
                .personalSchedules(personalSchedules)
                .build();

        cacheCalendar(cacheKey, response);
        return response;
    }

    @Override
    @Transactional
    public CalendarExportResponse export(Long userId) {
        Calendar client = buildGoogleCalendarClient(userId);

        List<CurriculumNodeCalendarSync> notSyncedNodes =
                curriculumNodeCalendarSyncRepository
                        .findByCurriculumNodeCurriculumUserUserIdAndSyncStatus(userId, SyncStatus.NOT_SYNCED);

        int exportedNodes = 0;
        for (CurriculumNodeCalendarSync sync : notSyncedNodes) {
            CurriculumNode node = sync.getCurriculumNode();
            try {
                Event event;
                if (sync.getGoogleEventId() == null) {
                    event = googleCalendarClientService.createAllDayEvent(
                            client,
                            node.getTitle(),
                            node.getDescription(),
                            node.getScheduledDate()
                    );
                } else {
                    event = googleCalendarClientService.updateAllDayEvent(
                            client,
                            sync.getGoogleEventId(),
                            node.getTitle(),
                            node.getDescription(),
                            node.getScheduledDate()
                    );
                }
                sync.synced("primary", event.getId(), event.getEtag());
                exportedNodes++;
            } catch (IOException e) {
                log.warn("커리큘럼 노드 Google Calendar 내보내기 실패. nodeId={}", node.getCurriculumNodeId(), e);
                sync.syncFailed();
            }
        }

        List<UserSchedule> unlinkedSchedules =
                userScheduleRepository.findByUserUserIdAndGoogleEventIdIsNull(userId);

        int exportedSchedules = 0;
        for (UserSchedule schedule : unlinkedSchedules) {
            try {
                Event event = googleCalendarClientService.createAllDayEvent(
                        client,
                        schedule.getTitle(),
                        schedule.getDescription(),
                        schedule.getStartDate(),
                        schedule.getEndDate()
                );
                schedule.updateGoogleEventId(event.getId());
                exportedSchedules++;
            } catch (IOException e) {
                log.warn("개인 일정 Google Calendar 내보내기 실패. scheduleId={}", schedule.getUserScheduleId(), e);
            }
        }

        log.info("Google Calendar 내보내기 완료. userId={}, nodes={}, schedules={}", userId, exportedNodes, exportedSchedules);
        return CalendarExportResponse.builder()
                .exportedNodes(exportedNodes)
                .exportedSchedules(exportedSchedules)
                .build();
    }

    @Override
    @Transactional
    public CalendarImportResponse importFromGoogle(Long userId) {
        Calendar client = buildGoogleCalendarClient(userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다. userId=" + userId));

        List<Event> googleEvents;
        try {
            googleEvents = googleCalendarClientService.listAllEvents(client);
        } catch (IOException e) {
            throw new IllegalStateException("Google Calendar 이벤트 조회 실패. userId=" + userId, e);
        }

        List<CalendarConflictInfo> conflictNodes = new ArrayList<>();
        int updatedSchedules = 0;
        int newSchedules = 0;

        for (Event event : googleEvents) {
            String googleEventId = event.getId();
            String googleEtag = event.getEtag();
            LocalDate googleStartDate = googleCalendarClientService.parseEventStartDate(event);
            LocalDate googleEndDate = googleCalendarClientService.parseEventEndDate(event);
            String title = event.getSummary();

            Optional<CurriculumNodeCalendarSync> syncOpt =
                    curriculumNodeCalendarSyncRepository.findByGoogleEventId(googleEventId);

            if (syncOpt.isPresent()) {
                CurriculumNodeCalendarSync sync = syncOpt.get();
                if (!Objects.equals(sync.getGoogleEtag(), googleEtag)) {
                    CurriculumNode node = sync.getCurriculumNode();
                    sync.updateEtag(googleEtag);
                    conflictNodes.add(CalendarConflictInfo.builder()
                            .curriculumNodeId(node.getCurriculumNodeId())
                            .ourTitle(node.getTitle())
                            .ourDescription(node.getDescription())
                            .ourDate(node.getScheduledDate())
                            .googleTitle(event.getSummary())
                            .googleDescription(event.getDescription())
                            .googleDate(googleStartDate)
                            .options(List.of("USE_GOOGLE", "USE_OURS", "DISCARD"))
                            .build());
                }
                continue;
            }

            Optional<UserSchedule> scheduleOpt =
                    userScheduleRepository.findByGoogleEventId(googleEventId);

            if (scheduleOpt.isPresent()) {
                UserSchedule schedule = scheduleOpt.get();
                boolean titleChanged = !Objects.equals(schedule.getTitle(), title);
                boolean dateChanged = !Objects.equals(schedule.getStartDate(), googleStartDate)
                        || !Objects.equals(schedule.getEndDate(), googleEndDate);

                if (titleChanged || dateChanged) {
                    schedule.update(
                            titleChanged ? title : null,
                            null,
                            dateChanged ? googleStartDate : null,
                            dateChanged ? googleEndDate : null
                    );
                    invalidateCalendarCache(userId, googleStartDate, googleEndDate);
                    updatedSchedules++;
                }
                continue;
            }

            UserSchedule newSchedule = UserSchedule.builder()
                    .user(user)
                    .title(title != null ? title : "")
                    .startDate(googleStartDate)
                    .endDate(googleEndDate)
                    .googleEventId(googleEventId)
                    .build();
            userScheduleRepository.save(newSchedule);
            invalidateCalendarCache(userId, googleStartDate, googleEndDate);
            newSchedules++;
        }

        log.info("Google Calendar 가져오기 완료. userId={}, conflicts={}, updated={}, new={}",
                userId, conflictNodes.size(), updatedSchedules, newSchedules);
        return CalendarImportResponse.builder()
                .conflictNodes(conflictNodes)
                .updatedSchedules(updatedSchedules)
                .newSchedules(newSchedules)
                .build();
    }

    private Calendar buildGoogleCalendarClient(Long userId) {
        OAuthAccount oAuthAccount = oAuthAccountRepository.findByUserUserIdAndProvider(userId, OAuthProvider.GOOGLE)
                .orElseThrow(() -> new GoogleOAuthNotFoundException(userId));
        String accessToken = oAuthTokenCryptoService.decrypt(oAuthAccount.getRefreshToken());
        return googleCalendarClientService.buildCalendarClient(accessToken);
    }

    private void invalidateCalendarCache(Long userId, LocalDate startDate, LocalDate endDate) {
        YearMonth start = YearMonth.from(startDate);
        YearMonth end = YearMonth.from(endDate);
        YearMonth current = start;
        while (!current.isAfter(end)) {
            redisService.delete("calendar:" + userId + ":" + current.getYear() + ":" + current.getMonthValue());
            current = current.plusMonths(1);
        }
    }

    private List<CalendarCurriculumResponse> buildCurriculaResponse(Long userId, LocalDate startDate, LocalDate endDate) {
        List<CurriculumNode> nodesInPeriod = curriculumNodeRepository
                .findByCurriculumUserUserIdAndScheduledDateBetween(userId, startDate, endDate);

        Map<Long, List<CurriculumNode>> nodesByCurriculumId = nodesInPeriod.stream()
                .collect(Collectors.groupingBy(n -> n.getCurriculum().getCurriculumId()));

        return nodesByCurriculumId.entrySet().stream()
                .map(entry -> {
                    Long curriculumId = entry.getKey();
                    List<CurriculumNode> periodNodes = entry.getValue();
                    Curriculum curriculum = periodNodes.get(0).getCurriculum();

                    // 줄기 연결선 렌더링용: 조회 기간 바깥의 인접 노드 날짜 산출
                    List<CurriculumNode> allNodes = curriculumNodeRepository
                            .findByCurriculumCurriculumIdOrderByScheduledDate(curriculumId);
                    LocalDate prevNodeDate = allNodes.stream()
                            .map(CurriculumNode::getScheduledDate)
                            .filter(d -> d.isBefore(startDate))
                            .max(Comparator.naturalOrder())
                            .orElse(null);
                    LocalDate nextNodeDate = allNodes.stream()
                            .map(CurriculumNode::getScheduledDate)
                            .filter(d -> d.isAfter(endDate))
                            .min(Comparator.naturalOrder())
                            .orElse(null);

                    List<CalendarNodeResponse> nodeResponses = periodNodes.stream()
                            .map(CalendarNodeResponse::from)
                            .toList();

                    return CalendarCurriculumResponse.builder()
                            .curriculumId(curriculumId)
                            .status(curriculum.getStatus())
                            .prevNodeDate(prevNodeDate)
                            .nextNodeDate(nextNodeDate)
                            .nodes(nodeResponses)
                            .build();
                })
                .toList();
    }

    private List<CalendarScheduleResponse> buildScheduleResponse(Long userId, LocalDate startDate, LocalDate endDate) {
        List<UserSchedule> schedules = userScheduleRepository
                .findByUserUserIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(userId, endDate, startDate);
        return schedules.stream()
                .map(CalendarScheduleResponse::from)
                .toList();
    }

    private void cacheCalendar(String cacheKey, CalendarResponse response) {
        try {
            redisService.save(cacheKey, objectMapper.writeValueAsString(response), 1L, TimeUnit.HOURS);
        } catch (JacksonException e) {
            log.warn("캘린더 캐시 저장 실패. key={}", cacheKey, e);
        }
    }
}
