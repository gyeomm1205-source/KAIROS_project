package com.ssafy.springbootbe.domain.calendar.service;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import com.ssafy.springbootbe.common.redis.RedisService;
import com.ssafy.springbootbe.domain.calendar.dto.response.CalendarCurriculumResponse;
import com.ssafy.springbootbe.domain.calendar.dto.response.CalendarNodeResponse;
import com.ssafy.springbootbe.domain.calendar.dto.response.CalendarPeriod;
import com.ssafy.springbootbe.domain.calendar.dto.response.CalendarResponse;
import com.ssafy.springbootbe.domain.calendar.dto.response.CalendarScheduleResponse;
import com.ssafy.springbootbe.persistence.curriculum.entity.Curriculum;
import com.ssafy.springbootbe.persistence.curriculum.entity.CurriculumNode;
import com.ssafy.springbootbe.persistence.curriculum.repository.CurriculumNodeRepository;
import com.ssafy.springbootbe.persistence.schedule.entity.UserSchedule;
import com.ssafy.springbootbe.persistence.schedule.repository.UserScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


@Slf4j
@Service
@RequiredArgsConstructor
public class CalendarServiceImpl implements CalendarService {

    private final CurriculumNodeRepository curriculumNodeRepository;
    private final UserScheduleRepository userScheduleRepository;
    private final RedisService redisService;
    private final ObjectMapper objectMapper;

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
