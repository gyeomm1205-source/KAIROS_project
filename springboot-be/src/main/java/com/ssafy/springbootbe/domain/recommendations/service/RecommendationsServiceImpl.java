package com.ssafy.springbootbe.domain.recommendations.service;

import com.ssafy.springbootbe.common.redis.RedisService;
import com.ssafy.springbootbe.common.utils.AIRestClient;
import com.ssafy.springbootbe.domain.recommendations.dto.request.DailyRecommendationGenerateRequest;
import com.ssafy.springbootbe.persistence.activity.repository.ActivityHistoryRepository;
import com.ssafy.springbootbe.persistence.activity.repository.ActivityHistoryTechStackRepository;
import com.ssafy.springbootbe.persistence.activity.type.ActivityType;
import com.ssafy.springbootbe.persistence.curriculum.entity.Curriculum;
import com.ssafy.springbootbe.persistence.curriculum.repository.CurriculumRepository;
import com.ssafy.springbootbe.persistence.curriculum.type.CurriculumStatus;
import com.ssafy.springbootbe.persistence.schedule.repository.UserScheduleRepository;
import com.ssafy.springbootbe.persistence.techstack.entity.TechStack;
import com.ssafy.springbootbe.persistence.user.entity.User;
import com.ssafy.springbootbe.persistence.user.entity.UserTechStack;
import com.ssafy.springbootbe.persistence.user.repository.UserTechStackRepository;
import com.ssafy.springbootbe.persistence.user.type.UserPosition;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationsServiceImpl implements RecommendationsService {

    static final long RECOMMENDATION_TTL_HOURS = 24L;
    private static final String RECOMMENDATION_KEY_PREFIX = "recommendation:";
    private static final String FAST_API_LEVEL_JUNIOR = "JUNIOR";
    private static final String FAST_API_LEVEL_MID = "MID";
    private static final String FAST_API_LEVEL_SENIOR = "SENIOR";

    private final CurriculumRepository curriculumRepository;
    private final UserTechStackRepository userTechStackRepository;
    private final ActivityHistoryRepository activityHistoryRepository;
    private final ActivityHistoryTechStackRepository activityHistoryTechStackRepository;
    private final UserScheduleRepository userScheduleRepository;
    private final RedisService redisService;
    private final AIRestClient aiRestClient;
    private final ObjectMapper objectMapper;

    @Value("${ai.server-url}")
    private String aiServerUrl;

    @Value("${ai.recommendations-daily-generate-path}")
    private String aiRecommendationsDailyGeneratePath;

    @Override
    @Transactional(readOnly = true)
    public void refreshDailyRecommendations() {
        List<Curriculum> activeCurricula = curriculumRepository.findByStatusOrderByCreatedAtDesc(CurriculumStatus.ACTIVE);
        int successCount = 0;

        for (Curriculum curriculum : activeCurricula) {
            Long curriculumId = curriculum.getCurriculumId();
            Long userId = curriculum.getUser().getUserId();

            try {
                DailyRecommendationGenerateRequest request = buildDailyGenerateRequest(curriculum);
                String responsePayload = requestDailyRecommendationPayload(request);
//                System.out.printf("=== %d ===\n", successCount+1);
//                System.out.printf("%s\n\n", responsePayload);
                cacheRecommendation(userId, curriculumId, responsePayload);
                successCount++;
            } catch (IllegalStateException e) {
                log.error("추천 배치 데이터 집계 실패. userId={}, curriculumId={}", userId, curriculumId, e);
            } catch (RestClientException e) {
                log.error("FastAPI 추천 생성 호출 실패. userId={}, curriculumId={}", userId, curriculumId, e);
            } catch (RuntimeException e) {
                log.error("Redis 추천 저장 실패. userId={}, curriculumId={}", userId, curriculumId, e);
            }
        }

        log.info("추천 배치 처리 완료. total={}, success={}", activeCurricula.size(), successCount);
    }

    DailyRecommendationGenerateRequest buildDailyGenerateRequest(Curriculum curriculum) {
        User user = curriculum.getUser();
        Long userId = user.getUserId();

        return DailyRecommendationGenerateRequest.builder()
                .userId(userId)
                .curriculumId(curriculum.getCurriculumId())
                .currentLevel(mapCurrentLevel(user.getPosition()))
                .favoriteTechStacks(buildFavoriteTechStacks(userId))
                .skillStats(buildSkillStats(userId))
                .recentActivities(buildRecentActivities(userId))
                .googleCalendarEvents(buildGoogleCalendarEvents(userId, user.getConsiderPersonalSchedule()))
                .recentCurriculaIds(buildRecentCurriculaIds(userId, curriculum.getCurriculumId()))
                .build();
    }

    String requestDailyRecommendationPayload(DailyRecommendationGenerateRequest request) {
        String response = aiRestClient.buildAiRestClient()
                .post()
                .uri(buildDailyGenerateUri())
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(String.class);

        if (response == null || response.isBlank()) {
            throw new IllegalStateException("FastAPI 추천 응답이 비어 있습니다.");
        }

        return response;
    }

    void cacheRecommendation(Long userId, Long curriculumId, String payload) {
        validateJsonPayload(payload);
        redisService.save(
                buildRecommendationKey(userId, curriculumId),
                payload,
                RECOMMENDATION_TTL_HOURS,
                TimeUnit.HOURS
        );
    }

    String buildDailyGenerateUri() {
        return aiServerUrl + aiRecommendationsDailyGeneratePath;
    }

    String mapCurrentLevel(UserPosition position) {
        if (position == null) {
            return FAST_API_LEVEL_MID;
        }

        return switch (position) {
            case STUDENT, JUNIOR -> FAST_API_LEVEL_JUNIOR;
            case SENIOR -> FAST_API_LEVEL_SENIOR;
            case JOB_SEEKER, OTHER -> FAST_API_LEVEL_MID;
        };
    }

    private List<String> buildFavoriteTechStacks(Long userId) {
        return userTechStackRepository.findByUserUserId(userId).stream()
                .map(UserTechStack::getTechStack)
                .sorted(Comparator.comparing(TechStack::getTechName))
                .map(TechStack::getTechName)
                .toList();
    }

    private List<DailyRecommendationGenerateRequest.SkillStat> buildSkillStats(Long userId) {
        return activityHistoryTechStackRepository.findIncludedTechStackCountsByUserId(userId).stream()
                .map(row -> DailyRecommendationGenerateRequest.SkillStat.builder()
                        .skill(((TechStack) row[0]).getTechName())
                        .count(((Number) row[1]).longValue())
                        .build())
                .toList();
    }

    private List<DailyRecommendationGenerateRequest.RecentActivity> buildRecentActivities(Long userId) {
        return activityHistoryRepository.findTop10ByUserUserIdAndIsIncludedTrueOrderByActivityDateDesc(userId).stream()
                .map(activity -> DailyRecommendationGenerateRequest.RecentActivity.builder()
                        .activityType(mapActivityType(activity.getActivityType()))
                        .category(activity.getCategory() == null ? null : activity.getCategory().name())
                        .title(activity.getTitle())
                        .description(activity.getDescription())
                        .activityDate(activity.getActivityDate())
                        .techStacks(buildActivityTechStacks(activity.getActivityHistoryId()))
                        .build())
                .toList();
    }

    private List<String> buildActivityTechStacks(Long activityHistoryId) {
        return activityHistoryTechStackRepository.findByActivityHistoryActivityHistoryId(activityHistoryId).stream()
                .map(techStack -> techStack.getTechStack().getTechName())
                .toList();
    }

    private List<DailyRecommendationGenerateRequest.GoogleCalendarEvent> buildGoogleCalendarEvents(
            Long userId,
            Boolean considerPersonalSchedule) {
        if (!Boolean.TRUE.equals(considerPersonalSchedule)) {
            return List.of();
        }

        return userScheduleRepository
                .findTop20ByUserUserIdAndEndDateGreaterThanEqualOrderByStartDateAsc(userId, LocalDate.now())
                .stream()
                .map(schedule -> DailyRecommendationGenerateRequest.GoogleCalendarEvent.builder()
                        .title(schedule.getTitle())
                        .startDate(schedule.getStartDate())
                        .endDate(schedule.getEndDate())
                        .build())
                .toList();
    }

    private List<Long> buildRecentCurriculaIds(Long userId, Long curriculumId) {
        return curriculumRepository.findByUserUserIdOrderByCreatedAtDesc(userId).stream()
                .map(Curriculum::getCurriculumId)
                .filter(id -> !id.equals(curriculumId))
                .limit(5)
                .toList();
    }

    private String buildRecommendationKey(Long userId, Long curriculumId) {
        return RECOMMENDATION_KEY_PREFIX + userId + ":" + curriculumId;
    }

    private String mapActivityType(ActivityType activityType) {
        if (activityType == ActivityType.GITHUB_COMMIT) {
            return "COMMIT";
        }
        return activityType.name();
    }

    private void validateJsonPayload(String payload) {
        try {
            objectMapper.readTree(payload);
        } catch (JacksonException e) {
            throw new IllegalStateException("추천 응답 JSON 검증에 실패했습니다.", e);
        }
    }
}
