package com.ssafy.springbootbe.domain.recommendations.service;

import com.ssafy.springbootbe.common.dto.TechStackInfo;
import com.ssafy.springbootbe.common.redis.RedisService;
import com.ssafy.springbootbe.common.utils.AIRestClient;
import com.ssafy.springbootbe.domain.recommendations.dto.request.DailyRecommendationGenerateRequest;
import com.ssafy.springbootbe.domain.recommendations.dto.response.RecommendationCachePayload;
import com.ssafy.springbootbe.domain.recommendations.dto.response.RecommendationDetailResponse;
import com.ssafy.springbootbe.domain.recommendations.dto.response.RecommendationListItemResponse;
import com.ssafy.springbootbe.domain.recommendations.dto.response.RecommendationListResponse;
import com.ssafy.springbootbe.domain.recommendations.exception.RecommendationAccessDeniedException;
import com.ssafy.springbootbe.domain.recommendations.exception.RecommendationCurriculumNotFoundException;
import com.ssafy.springbootbe.domain.recommendations.exception.RecommendationCurriculumRetrievalException;
import com.ssafy.springbootbe.domain.recommendations.exception.RecommendationDetailNotFoundException;
import com.ssafy.springbootbe.domain.recommendations.exception.RecommendationNodeAggregationException;
import com.ssafy.springbootbe.domain.recommendations.exception.RecommendationPayloadParsingException;
import com.ssafy.springbootbe.domain.recommendations.exception.RecommendationQuizGenerationException;
import com.ssafy.springbootbe.domain.recommendations.exception.RecommendationRedisLookupException;
import com.ssafy.springbootbe.domain.quizzes.dto.request.QuizGenerateAsyncRequest;
import com.ssafy.springbootbe.domain.quizzes.dto.response.QuizGenerateAsyncResponse;
import com.ssafy.springbootbe.persistence.activity.entity.ActivityHistory;
import com.ssafy.springbootbe.persistence.activity.entity.ActivityHistoryTechStack;
import com.ssafy.springbootbe.persistence.activity.repository.ActivityHistoryRepository;
import com.ssafy.springbootbe.persistence.activity.repository.ActivityHistoryTechStackRepository;
import com.ssafy.springbootbe.persistence.activity.type.ActivityType;
import com.ssafy.springbootbe.persistence.curriculum.entity.Curriculum;
import com.ssafy.springbootbe.persistence.curriculum.entity.CurriculumNode;
import com.ssafy.springbootbe.persistence.curriculum.entity.CurriculumTechStack;
import com.ssafy.springbootbe.persistence.curriculum.repository.CurriculumNodeRepository;
import com.ssafy.springbootbe.persistence.curriculum.repository.CurriculumRepository;
import com.ssafy.springbootbe.persistence.curriculum.repository.CurriculumTechStackRepository;
import com.ssafy.springbootbe.persistence.curriculum.type.CurriculumStatus;
import com.ssafy.springbootbe.persistence.schedule.repository.UserScheduleRepository;
import com.ssafy.springbootbe.persistence.techstack.entity.TechStack;
import com.ssafy.springbootbe.persistence.techstack.repository.TechStackRepository;
import com.ssafy.springbootbe.persistence.user.entity.User;
import com.ssafy.springbootbe.persistence.user.entity.UserTechStack;
import com.ssafy.springbootbe.persistence.user.repository.UserTechStackRepository;
import com.ssafy.springbootbe.persistence.user.type.UserPosition;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationsServiceImpl implements RecommendationsService {

    static final long RECOMMENDATION_TTL_HOURS = 24L;
    private static final DateTimeFormatter MONTH_DAY_FORMATTER = DateTimeFormatter.ofPattern("M/d");
    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");
    private static final String RECOMMENDATION_KEY_PREFIX = "recommendation:";
    private static final String RECOMMENDATION_QUIZ_KEY_PREFIX = "recommendation:quiz:";
    private static final String FAST_API_LEVEL_JUNIOR = "JUNIOR";
    private static final String FAST_API_LEVEL_MID = "MID";
    private static final String FAST_API_LEVEL_SENIOR = "SENIOR";

    private final CurriculumRepository curriculumRepository;
    private final CurriculumNodeRepository curriculumNodeRepository;
    private final CurriculumTechStackRepository curriculumTechStackRepository;
    private final UserTechStackRepository userTechStackRepository;
    private final TechStackRepository techStackRepository;
    private final ActivityHistoryRepository activityHistoryRepository;
    private final ActivityHistoryTechStackRepository activityHistoryTechStackRepository;
    private final UserScheduleRepository userScheduleRepository;
    private final RedisService redisService;
    private final AIRestClient aiRestClient;
    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, Object> referenceHistoryLocks = new ConcurrentHashMap<>();

    @Value("${ai.server-url}")
    private String aiServerUrl;

    @Value("${ai.recommendations-daily-generate-path}")
    private String aiRecommendationsDailyGeneratePath;

    @Value("${ai.quizzes-generate-async-path:/api/v1/ai/quizzes/generate-async}")
    private String aiQuizzesGenerateAsyncPath;

    @Override
    @Transactional(readOnly = true)
    public RecommendationListResponse findRecommendations(Long userId) {
        List<Curriculum> curricula = findCurricula(userId);
        Map<Long, List<CurriculumNode>> nodesByCurriculumId = aggregateCurriculumNodes(userId, curricula);
        Map<Long, List<TechStackInfo>> techStacksByCurriculumId = aggregateCurriculumTechStacks(curricula);

        List<RecommendationListItemResponse> items = curricula.stream()
                .map(curriculum -> {
                    RecommendationCachePayload recommendationPayload = findRecommendationPayloadOrNull(
                            userId,
                            curriculum.getCurriculumId()
                    );

                    return RecommendationListItemResponse.builder()
                            .curriculumId(curriculum.getCurriculumId())
                            .status(curriculum.getStatus())
                            .displayName(buildDisplayName(
                                    nodesByCurriculumId.get(curriculum.getCurriculumId()),
                                    curriculum.getCurriculumId()
                            ))
                            .startDate(findStartDate(nodesByCurriculumId.get(curriculum.getCurriculumId())))
                            .endDate(findEndDate(nodesByCurriculumId.get(curriculum.getCurriculumId())))
                            .techStacks(resolveCurriculumTechStacks(
                                    techStacksByCurriculumId.get(curriculum.getCurriculumId()),
                                    recommendationPayload
                            ))
                            .hasRecommendation(recommendationPayload != null)
                            .build();
                })
                .toList();

        return RecommendationListResponse.builder()
                .items(items)
                .build();
    }

    @Override
    @Transactional
    public RecommendationDetailResponse findRecommendationDetail(Long userId, Long curriculumId) {
        Curriculum curriculum = findCurriculumOrThrow(curriculumId);
        validateCurriculumOwnership(userId, curriculum);

        RecommendationCachePayload recommendationPayload = findRecommendationPayload(userId, curriculumId);
        QuizGenerateAsyncResponse quizPayload = findOrGenerateQuizPayload(
                userId,
                curriculum,
                recommendationPayload
        );
        saveReferenceActivityHistory(curriculum, recommendationPayload);

        return RecommendationDetailResponse.builder()
                .curriculumId(curriculumId)
                .recommendationReason(mapRecommendationReason(recommendationPayload))
                .currentStatus(mapCurrentStatus(recommendationPayload))
                .quizzes(List.of(mapQuiz(quizPayload)))
                .references(mapReferences(recommendationPayload))
                .nextNodes(mapNextNodes(recommendationPayload))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public void refreshDailyRecommendations() {
        List<Curriculum> activeCurricula = curriculumRepository.findByStatusOrderByCreatedAtDesc(CurriculumStatus.ACTIVE);
        int successCount = 0;

        for (Curriculum curriculum : activeCurricula) {
            try {
                generateAndCacheDailyRecommendation(curriculum);
                successCount++;
            } catch (RuntimeException e) {
                log.error("추천 배치 처리 실패. userId={}, curriculumId={}",
                        curriculum.getUser().getUserId(),
                        curriculum.getCurriculumId(),
                        e);
            }
        }

        log.info("추천 배치 처리 완료. total={}, success={}", activeCurricula.size(), successCount);
    }

    @Override
    @Transactional(readOnly = true)
    @Async
    public void refreshDailyRecommendation(Long userId, Long curriculumId) {
        Curriculum curriculum = findCurriculumOrThrow(curriculumId);
        validateCurriculumOwnership(userId, curriculum);

        try {
            generateAndCacheDailyRecommendation(curriculum);
            log.info("추천 단건 처리 완료. userId={}, curriculumId={}", userId, curriculumId);
        } catch (RuntimeException e) {
            log.error("추천 단건 처리 실패. userId={}, curriculumId={}", userId, curriculumId, e);
            throw e;
        }
    }

    private void generateAndCacheDailyRecommendation(Curriculum curriculum) {
        Long userId = curriculum.getUser().getUserId();
        Long curriculumId = curriculum.getCurriculumId();

        DailyRecommendationGenerateRequest request = buildDailyGenerateRequest(curriculum);
        String responsePayload = requestDailyRecommendationPayload(request);
        cacheRecommendation(userId, curriculumId, responsePayload);
    }

    DailyRecommendationGenerateRequest buildDailyGenerateRequest(Curriculum curriculum) {
        User user = curriculum.getUser();
        Long userId = user.getUserId();
        CurriculumNode currentNode = findCurrentNode(curriculum.getCurriculumId());
        List<String> currentNodeTechStacks = resolveCurrentNodeTechStacks(curriculum.getCurriculumId());

        return DailyRecommendationGenerateRequest.builder()
                .userId(userId)
                .curriculumId(curriculum.getCurriculumId())
                .currentLevel(mapCurrentLevel(user.getPosition()))
                .favoriteTechStacks(buildFavoriteTechStacks(userId))
                .skillStats(buildSkillStats(userId))
                .recentActivities(buildRecentActivities(userId))
                .googleCalendarEvents(buildGoogleCalendarEvents(userId, user.getConsiderPersonalSchedule()))
                .recentCurriculaIds(buildRecentCurriculaIds(userId, curriculum.getCurriculumId()))
                .currentNodeTitle(currentNode == null ? null : currentNode.getTitle())
                .currentNodeDescription(currentNode == null ? null : currentNode.getDescription())
                .currentNodeDate(currentNode == null ? null : currentNode.getScheduledDate())
                .currentNodeTechStacks(currentNodeTechStacks)
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

    QuizGenerateAsyncResponse requestQuizGeneration(QuizGenerateAsyncRequest request) {
        try {
            QuizGenerateAsyncResponse response = aiRestClient.buildAiRestClient()
                    .post()
                    .uri(buildQuizGenerateUri())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(QuizGenerateAsyncResponse.class);

            validateQuizPayload(response);
            return response;
        } catch (RestClientException e) {
            throw new RecommendationQuizGenerationException("FastAPI 퀴즈 생성 호출에 실패했습니다.", e);
        }
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

    String buildQuizGenerateUri() {
        return aiServerUrl + aiQuizzesGenerateAsyncPath;
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
                        .category(activity.getCategory() == null ? "기타" : activity.getCategory().name())
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

    private String buildRecommendationQuizKey(Long userId, Long curriculumId) {
        return RECOMMENDATION_QUIZ_KEY_PREFIX + userId + ":" + curriculumId;
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

    private List<Curriculum> findCurricula(Long userId) {
        try {
            return curriculumRepository.findByUserUserIdOrderByCreatedAtDesc(userId);
        } catch (RuntimeException e) {
            throw new RecommendationCurriculumRetrievalException(userId, e);
        }
    }

    private Map<Long, List<CurriculumNode>> aggregateCurriculumNodes(Long userId, List<Curriculum> curricula) {
        if (curricula.isEmpty()) {
            return Map.of();
        }

        try {
            List<Long> curriculumIds = curricula.stream()
                    .map(Curriculum::getCurriculumId)
                    .toList();

            Map<Long, List<CurriculumNode>> nodesByCurriculumId = new HashMap<>();
            curriculumNodeRepository
                    .findByCurriculumCurriculumIdInOrderByCurriculumCurriculumIdAscScheduledDateAsc(curriculumIds)
                    .forEach(node -> nodesByCurriculumId
                            .computeIfAbsent(node.getCurriculum().getCurriculumId(), ignored -> new java.util.ArrayList<>())
                            .add(node));
            return nodesByCurriculumId;
        } catch (RuntimeException e) {
            throw new RecommendationNodeAggregationException(userId, e);
        }
    }

    private Map<Long, List<TechStackInfo>> aggregateCurriculumTechStacks(List<Curriculum> curricula) {
        if (curricula.isEmpty()) {
            return Map.of();
        }

        List<Long> curriculumIds = curricula.stream()
                .map(Curriculum::getCurriculumId)
                .toList();

        Map<Long, List<TechStackInfo>> techStacksByCurriculumId = new HashMap<>();
        for (CurriculumTechStack link : curriculumTechStackRepository.findByCurriculumCurriculumIdIn(curriculumIds)) {
            techStacksByCurriculumId
                    .computeIfAbsent(link.getCurriculum().getCurriculumId(), ignored -> new ArrayList<>())
                    .add(TechStackInfo.from(link.getTechStack()));
        }
        return techStacksByCurriculumId;
    }

    private List<TechStackInfo> resolveCurriculumTechStacks(
            List<TechStackInfo> persistedTechStacks,
            RecommendationCachePayload recommendationPayload) {
        if (persistedTechStacks != null && !persistedTechStacks.isEmpty()) {
            return persistedTechStacks;
        }

        if (recommendationPayload == null
                || recommendationPayload.getCurrentStatus() == null
                || recommendationPayload.getCurrentStatus().getTopSkills() == null) {
            return List.of();
        }

        return recommendationPayload.getCurrentStatus().getTopSkills().stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .map(this::toTechStackInfo)
                .limit(5)
                .toList();
    }

    private TechStackInfo toTechStackInfo(String techName) {
        return techStackRepository.findByTechNameIgnoreCase(techName)
                .map(TechStackInfo::from)
                .orElseGet(() -> TechStackInfo.builder()
                        .techName(techName)
                        .build());
    }

    private LocalDate findStartDate(List<CurriculumNode> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return null;
        }
        return nodes.getFirst().getScheduledDate();
    }

    private LocalDate findEndDate(List<CurriculumNode> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return null;
        }
        return nodes.getLast().getScheduledDate();
    }

    private String buildDisplayName(List<CurriculumNode> nodes, Long curriculumId) {
        if (nodes == null || nodes.isEmpty()) {
            return "커리큘럼 " + curriculumId;
        }

        LocalDate startDate = nodes.getFirst().getScheduledDate();
        LocalDate endDate = nodes.getLast().getScheduledDate();
        String title = nodes.getFirst().getTitle();

        if (startDate == null || endDate == null) {
            return title;
        }

        return title + " · " + formatMonthDay(startDate) + "~" + formatMonthDay(endDate);
    }

    private String formatMonthDay(LocalDate date) {
        return date.format(MONTH_DAY_FORMATTER);
    }

    private boolean hasRecommendation(Long userId, Long curriculumId) {
        try {
            return redisService.hasKey(buildRecommendationKey(userId, curriculumId));
        } catch (RuntimeException e) {
            throw new RecommendationRedisLookupException(userId, curriculumId, e);
        }
    }

    private RecommendationCachePayload findRecommendationPayloadOrNull(Long userId, Long curriculumId) {
        try {
            String payload = redisService.get(buildRecommendationKey(userId, curriculumId));
            if (payload == null || payload.isBlank()) {
                return null;
            }

            return objectMapper.readValue(payload, RecommendationCachePayload.class);
        } catch (JacksonException e) {
            throw new RecommendationPayloadParsingException(
                    "추천 목록 Redis payload 파싱에 실패했습니다. curriculumId=" + curriculumId,
                    e
            );
        } catch (RuntimeException e) {
            throw new RecommendationRedisLookupException(userId, curriculumId, e);
        }
    }

    private Curriculum findCurriculumOrThrow(Long curriculumId) {
        return curriculumRepository.findById(curriculumId)
                .orElseThrow(() -> new RecommendationCurriculumNotFoundException(curriculumId));
    }

    private void validateCurriculumOwnership(Long userId, Curriculum curriculum) {
        if (!Objects.equals(curriculum.getUser().getUserId(), userId)) {
            throw new RecommendationAccessDeniedException(curriculum.getCurriculumId());
        }
    }

    private RecommendationCachePayload findRecommendationPayload(Long userId, Long curriculumId) {
        try {
            String payload = redisService.get(buildRecommendationKey(userId, curriculumId));
            if (payload == null || payload.isBlank()) {
                throw new RecommendationDetailNotFoundException(curriculumId);
            }

            return objectMapper.readValue(payload, RecommendationCachePayload.class);
        } catch (RecommendationDetailNotFoundException e) {
            throw e;
        } catch (JacksonException e) {
            throw new RecommendationPayloadParsingException(
                    "추천 상세 Redis payload 파싱에 실패했습니다. curriculumId=" + curriculumId,
                    e
            );
        } catch (RuntimeException e) {
            throw new RecommendationRedisLookupException(userId, curriculumId, e);
        }
    }

    private QuizGenerateAsyncResponse findOrGenerateQuizPayload(
            Long userId,
            Curriculum curriculum,
            RecommendationCachePayload recommendationPayload) {
        String quizKey = buildRecommendationQuizKey(userId, curriculum.getCurriculumId());

        try {
            String cachedQuizPayload = redisService.get(quizKey);
            if (cachedQuizPayload != null && !cachedQuizPayload.isBlank()) {
                return objectMapper.readValue(cachedQuizPayload, QuizGenerateAsyncResponse.class);
            }
        } catch (JacksonException e) {
            throw new RecommendationPayloadParsingException(
                    "추천 퀴즈 Redis payload 파싱에 실패했습니다. curriculumId=" + curriculum.getCurriculumId(),
                    e
            );
        } catch (RuntimeException e) {
            throw new RecommendationRedisLookupException(userId, curriculum.getCurriculumId(), e);
        }

        CurriculumNode currentNode = findCurrentNode(curriculum.getCurriculumId());
        QuizGenerateAsyncRequest request = QuizGenerateAsyncRequest.builder()
                .curriculumId(curriculum.getCurriculumId())
                .targetTechStacks(buildTargetTechStacks(userId, recommendationPayload))
                .userLevel(mapCurrentLevel(curriculum.getUser().getPosition()))
                .currentNodeTitle(currentNode == null ? null : currentNode.getTitle())
                .currentNodeDescription(currentNode == null ? null : currentNode.getDescription())
                .currentNodeDate(currentNode == null ? null : currentNode.getScheduledDate())
                .build();

        QuizGenerateAsyncResponse generatedQuiz = requestQuizGeneration(request);
        cacheRecommendationQuiz(userId, curriculum.getCurriculumId(), generatedQuiz);
        return generatedQuiz;
    }

    private List<String> buildTargetTechStacks(Long userId, RecommendationCachePayload recommendationPayload) {
        LinkedHashSet<String> techStacks = new LinkedHashSet<>();

        if (recommendationPayload.getCurrentStatus() != null
                && recommendationPayload.getCurrentStatus().getTopSkills() != null) {
            recommendationPayload.getCurrentStatus().getTopSkills().stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(value -> !value.isBlank())
                    .forEach(techStacks::add);
        }

        if (techStacks.isEmpty()) {
            userTechStackRepository.findTop6ByUserUserIdOrderByScoreDesc(userId).stream()
                    .map(UserTechStack::getTechStack)
                    .map(TechStack::getTechName)
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(value -> !value.isBlank())
                    .forEach(techStacks::add);
        }

        if (techStacks.isEmpty()) {
            userTechStackRepository.findByUserUserId(userId).stream()
                    .map(UserTechStack::getTechStack)
                    .map(TechStack::getTechName)
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(value -> !value.isBlank())
                    .forEach(techStacks::add);
        }

        if (techStacks.isEmpty()) {
            throw new RecommendationQuizGenerationException(
                    "퀴즈 생성용 targetTechStacks를 구성할 수 없습니다. userId=" + userId
            );
        }

        return techStacks.stream().limit(5).toList();
    }

    private CurriculumNode findCurrentNode(Long curriculumId) {
        List<CurriculumNode> nodes = curriculumNodeRepository.findByCurriculumCurriculumIdOrderByScheduledDate(curriculumId);
        if (nodes.isEmpty()) {
            return null;
        }

        LocalDate today = LocalDate.now();
        for (CurriculumNode node : nodes) {
            if (today.equals(node.getScheduledDate())) {
                return node;
            }
        }

        for (CurriculumNode node : nodes) {
            if (node.getScheduledDate() != null && node.getScheduledDate().isAfter(today)) {
                return node;
            }
        }

        return nodes.get(nodes.size() - 1);
    }

    private List<String> resolveCurrentNodeTechStacks(Long curriculumId) {
        List<TechStackInfo> techStacks = aggregateCurriculumTechStacks(
                List.of(findCurriculumOrThrow(curriculumId))
        ).get(curriculumId);

        if (techStacks == null || techStacks.isEmpty()) {
            return List.of();
        }

        return techStacks.stream()
                .map(TechStackInfo::getTechName)
                .filter(Objects::nonNull)
                .toList();
    }

    private void cacheRecommendationQuiz(Long userId, Long curriculumId, QuizGenerateAsyncResponse quizPayload) {
        try {
            redisService.save(
                    buildRecommendationQuizKey(userId, curriculumId),
                    objectMapper.writeValueAsString(quizPayload),
                    calculateQuizCacheTtlSeconds(),
                    TimeUnit.SECONDS
            );
        } catch (JacksonException e) {
            throw new RecommendationPayloadParsingException(
                    "추천 퀴즈 Redis 저장 직렬화에 실패했습니다. curriculumId=" + curriculumId,
                    e
            );
        } catch (RuntimeException e) {
            throw new RecommendationRedisLookupException(userId, curriculumId, e);
        }
    }

    long calculateQuizCacheTtlSeconds() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay();
        return Math.max(1L, Duration.between(now, nextMidnight).getSeconds());
    }

    private void validateQuizPayload(QuizGenerateAsyncResponse response) {
        if (response == null) {
            throw new RecommendationQuizGenerationException("FastAPI 퀴즈 생성 응답이 비어 있습니다.");
        }

        if (response.getQuestions() == null || response.getQuestions().isEmpty()) {
            throw new RecommendationQuizGenerationException("FastAPI 퀴즈 생성 응답에 questions가 없습니다.");
        }
    }

    private RecommendationDetailResponse.RecommendationReason mapRecommendationReason(
            RecommendationCachePayload recommendationPayload) {
        RecommendationCachePayload.RecommendationReason reason = recommendationPayload.getRecommendationReason();
        if (reason == null) {
            return null;
        }

        return RecommendationDetailResponse.RecommendationReason.builder()
                .summary(reason.getSummary())
                .detail(reason.getDetail())
                .build();
    }

    private RecommendationDetailResponse.RecommendationCurrentStatus mapCurrentStatus(
            RecommendationCachePayload recommendationPayload) {
        RecommendationCachePayload.CurrentStatus currentStatus = recommendationPayload.getCurrentStatus();
        if (currentStatus == null) {
            return null;
        }

        return RecommendationDetailResponse.RecommendationCurrentStatus.builder()
                .summary(currentStatus.getSummary())
                .detail(currentStatus.getDetail())
                .topSkills(currentStatus.getTopSkills() == null ? List.of() : currentStatus.getTopSkills())
                .build();
    }

    private RecommendationDetailResponse.RecommendationQuiz mapQuiz(QuizGenerateAsyncResponse quizPayload) {
        return RecommendationDetailResponse.RecommendationQuiz.builder()
                .title(quizPayload.getTitle())
                .description(quizPayload.getDescription())
                .expectedMinutes(quizPayload.getExpectedMinutes())
                .totalQuestions(quizPayload.getTotalQuestions())
                .questions(mapQuizQuestions(quizPayload))
                .build();
    }

    private List<RecommendationDetailResponse.RecommendationQuizQuestion> mapQuizQuestions(
            QuizGenerateAsyncResponse quizPayload) {
        List<RecommendationDetailResponse.RecommendationQuizQuestion> questions = new ArrayList<>();
        for (QuizGenerateAsyncResponse.Question question : quizPayload.getQuestions()) {
            questions.add(RecommendationDetailResponse.RecommendationQuizQuestion.builder()
                    .questionNumber(question.getQuestionNumber())
                    .question(question.getQuestion())
                    .quizType(question.getQuizType())
                    .options(question.getOptions())
                    .build());
        }
        return questions;
    }

    private List<RecommendationDetailResponse.RecommendationReference> mapReferences(
            RecommendationCachePayload recommendationPayload) {
        if (recommendationPayload.getReferences() == null) {
            return List.of();
        }

        return recommendationPayload.getReferences().stream()
                .map(reference -> RecommendationDetailResponse.RecommendationReference.builder()
                        .referenceId(reference.getReferenceId())
                        .title(reference.getTitle())
                        .recommendationReason(reference.getRecommendationReason())
                        .referenceType(reference.getReferenceType())
                        .publishedAt(reference.getPublishedAt())
                        .url(reference.getUrl())
                        .techStacks(reference.getTechStacks() == null ? List.of() : reference.getTechStacks())
                        .build())
                .toList();
    }

    private List<RecommendationDetailResponse.RecommendationNextNode> mapNextNodes(
            RecommendationCachePayload recommendationPayload) {
        if (recommendationPayload.getNextNodes() == null) {
            return List.of();
        }

        return recommendationPayload.getNextNodes().stream()
                .map(nextNode -> RecommendationDetailResponse.RecommendationNextNode.builder()
                        .title(nextNode.getTitle())
                        .build())
                .toList();
    }

    private void saveReferenceActivityHistory(Curriculum curriculum, RecommendationCachePayload recommendationPayload) {
        if (recommendationPayload.getReferences() == null || recommendationPayload.getReferences().isEmpty()) {
            return;
        }

        Long userId = curriculum.getUser().getUserId();
        Long curriculumId = curriculum.getCurriculumId();
        LocalDateTime now = LocalDateTime.now(SEOUL_ZONE);
        LocalDate today = now.toLocalDate();
        String lockKey = userId + ":" + curriculumId + ":" + today;
        Object lock = referenceHistoryLocks.computeIfAbsent(lockKey, key -> new Object());

        synchronized (lock) {
            if (activityHistoryRepository.existsByUserUserIdAndActivityTypeAndCurriculumIdAndActivityDateBetween(
                    userId,
                    ActivityType.REFERENCE,
                    curriculumId,
                    today.atStartOfDay(),
                    today.plusDays(1).atStartOfDay().minusNanos(1)
            )) {
                return;
            }

            List<CurriculumNode> nodes = curriculumNodeRepository
                    .findByCurriculumCurriculumIdOrderByScheduledDate(curriculumId);
            String displayName = buildDisplayName(nodes, curriculumId);
            String title = "레퍼런스 추천 — " + displayName;
            String description = buildReferenceHistoryDescription(recommendationPayload.getReferences());

            ActivityHistory activityHistory = activityHistoryRepository.save(ActivityHistory.builder()
                    .user(curriculum.getUser())
                    .activityType(ActivityType.REFERENCE)
                    .curriculumId(curriculumId)
                    .title(title)
                    .description(description)
                    .activityDate(now)
                    .build());

            List<ActivityHistoryTechStack> links = new ArrayList<>();
            recommendationPayload.getReferences().stream()
                    .map(RecommendationCachePayload.ReferenceItem::getTechStacks)
                    .filter(Objects::nonNull)
                    .flatMap(List::stream)
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(value -> !value.isBlank())
                    .distinct()
                    .forEach(techName -> techStackRepository.findByTechNameIgnoreCase(techName)
                            .ifPresent(techStack -> links.add(ActivityHistoryTechStack.builder()
                                    .activityHistory(activityHistory)
                                    .techStack(techStack)
                                    .build())));

            if (!links.isEmpty()) {
                activityHistoryTechStackRepository.saveAll(links);
            }
        }
    }

    private String buildReferenceHistoryDescription(List<RecommendationCachePayload.ReferenceItem> references) {
        List<String> lines = new ArrayList<>();

        for (RecommendationCachePayload.ReferenceItem reference : references.stream().filter(Objects::nonNull).limit(2).toList()) {
            String typeLabel = resolveReferenceTypeLabel(reference.getReferenceType());
            String title = reference.getTitle() == null ? "" : reference.getTitle().trim();
            if (title.isBlank()) {
                continue;
            }
            lines.add(typeLabel + " : " + title);
        }

        if (lines.isEmpty()) {
            return "추천 레퍼런스를 확인했습니다.";
        }

        if (references.size() > lines.size()) {
            lines.add("외 " + (references.size() - lines.size()) + "개");
        }

        return String.join("\n", lines);
    }

    private String resolveReferenceTypeLabel(String referenceType) {
        if (referenceType == null) {
            return "레퍼런스";
        }

        return switch (referenceType.toUpperCase()) {
            case "OFFICIAL_DOCS" -> "공식 문서";
            case "TECH_BLOG" -> "기술 블로그";
            case "WIKI" -> "위키";
            case "VIDEO" -> "영상";
            default -> "레퍼런스";
        };
    }
}
