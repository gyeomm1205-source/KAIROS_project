package com.ssafy.springbootbe.domain.curricula.service;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.ssafy.springbootbe.common.redis.RedisService;
import com.ssafy.springbootbe.common.utils.AIRestClient;
import com.ssafy.springbootbe.common.utils.OAuthTokenCryptoService;
import com.ssafy.springbootbe.domain.calendar.service.GoogleCalendarClientService;
import com.ssafy.springbootbe.domain.curricula.dto.request.CurriculumConfirmRequest;
import com.ssafy.springbootbe.domain.curricula.dto.request.CurriculumGenerateRequest;
import com.ssafy.springbootbe.domain.curricula.dto.request.CurriculumNodeUpdateRequest;
import com.ssafy.springbootbe.domain.curricula.dto.request.CurriculumPreviewRequest;
import com.ssafy.springbootbe.domain.curricula.dto.request.GoogleCalendarEventDto;
import com.ssafy.springbootbe.domain.curricula.dto.request.RecentActivityDto;
import com.ssafy.springbootbe.domain.curricula.dto.request.SkillStatDto;
import com.ssafy.springbootbe.domain.curricula.dto.response.CalendarSyncDto;
import com.ssafy.springbootbe.domain.curricula.dto.response.ConfirmNodeDto;
import com.ssafy.springbootbe.domain.curricula.dto.response.CurriculumConfirmResponse;
import com.ssafy.springbootbe.domain.curricula.dto.response.CurriculumGenerateResponse;
import com.ssafy.springbootbe.domain.curricula.dto.response.CurriculumNodeResponse;
import com.ssafy.springbootbe.domain.curricula.dto.response.CurriculumPreviewResponse;
import com.ssafy.springbootbe.domain.curricula.dto.response.CurriculumReasonResponse;
import com.ssafy.springbootbe.domain.curricula.dto.response.PreviewNodeDto;
import com.ssafy.springbootbe.domain.curricula.dto.response.PreviewOptionDto;
import com.ssafy.springbootbe.domain.curricula.dto.response.PreviewReasonDto;
import com.ssafy.springbootbe.domain.curricula.exception.CurriculumAccessDeniedException;
import com.ssafy.springbootbe.domain.curricula.exception.CurriculumNodeAccessDeniedException;
import com.ssafy.springbootbe.domain.curricula.exception.CurriculumNodeNotFoundException;
import com.ssafy.springbootbe.domain.curricula.exception.CurriculumNotFoundException;
import com.ssafy.springbootbe.persistence.activity.repository.ActivityHistoryRepository;
import com.ssafy.springbootbe.persistence.activity.repository.ActivityHistoryTechStackRepository;
import com.ssafy.springbootbe.persistence.activity.type.ActivityType;
import com.ssafy.springbootbe.persistence.curriculum.entity.Curriculum;
import com.ssafy.springbootbe.persistence.curriculum.entity.CurriculumNode;
import com.ssafy.springbootbe.persistence.curriculum.entity.CurriculumNodeCalendarSync;
import com.ssafy.springbootbe.persistence.curriculum.entity.CurriculumRecommendationReason;
import com.ssafy.springbootbe.persistence.curriculum.entity.CurriculumTechStack;
import com.ssafy.springbootbe.persistence.curriculum.repository.CurriculumNodeCalendarSyncRepository;
import com.ssafy.springbootbe.persistence.curriculum.repository.CurriculumNodeRepository;
import com.ssafy.springbootbe.persistence.curriculum.repository.CurriculumRecommendationReasonRepository;
import com.ssafy.springbootbe.persistence.curriculum.repository.CurriculumRepository;
import com.ssafy.springbootbe.persistence.curriculum.repository.CurriculumTechStackRepository;
import com.ssafy.springbootbe.persistence.curriculum.type.CurriculumStatus;
import com.ssafy.springbootbe.persistence.oauth.entity.OAuthAccount;
import com.ssafy.springbootbe.persistence.oauth.repository.OAuthAccountRepository;
import com.ssafy.springbootbe.persistence.oauth.type.OAuthProvider;
import com.ssafy.springbootbe.persistence.techstack.entity.TechStack;
import com.ssafy.springbootbe.persistence.techstack.repository.TechStackRepository;
import com.ssafy.springbootbe.persistence.user.entity.User;
import com.ssafy.springbootbe.persistence.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class CurriculaServiceImpl implements CurriculaService {

    private final CurriculumRepository curriculumRepository;
    private final CurriculumNodeRepository curriculumNodeRepository;
    private final CurriculumNodeCalendarSyncRepository curriculumNodeCalendarSyncRepository;
    private final CurriculumRecommendationReasonRepository curriculumRecommendationReasonRepository;
    private final CurriculumTechStackRepository curriculumTechStackRepository;
    private final UserRepository userRepository;
    private final ActivityHistoryRepository activityHistoryRepository;
    private final ActivityHistoryTechStackRepository activityHistoryTechStackRepository;
    private final OAuthAccountRepository oAuthAccountRepository;
    private final TechStackRepository techStackRepository;
    private final GoogleCalendarClientService googleCalendarClientService;
    private final OAuthTokenCryptoService oAuthTokenCryptoService;
    private final AIRestClient aiRestClient;
    private final RedisService redisService;
    private final ObjectMapper objectMapper;

    @Value("${ai.server-url}")
    private String aiServerUrl;

    @Value("${ai.curriculum-generate-path:/api/v1/ai/curriculum/generate}")
    private String curriculumGeneratePath;

    @Override
    @Transactional(readOnly = true)
    public CurriculumPreviewResponse preview(Long userId, CurriculumPreviewRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다. userId=" + userId));

        List<GoogleCalendarEventDto> googleEvents = new ArrayList<>();
        if (Boolean.TRUE.equals(user.getConsiderPersonalSchedule())) {
            googleEvents = fetchGoogleCalendarEvents(userId);
        }

        List<SkillStatDto> userTechStacks = buildUserTechStacks(userId);

        boolean isOnboardingPreview = request.getAnalysisData() != null;

        CurriculumGenerateRequest aiRequest = CurriculumGenerateRequest.builder()
                .userId(userId)
                .curriculumType(isOnboardingPreview ? "ONBOARDING" : "AUTO")
                .considerPersonalSchedule(user.getConsiderPersonalSchedule())
                .googleCalendarEvents(googleEvents)
                .analysisData(request.getAnalysisData())
                .userTechStacks(userTechStacks)
                .recentActivities(isOnboardingPreview ? List.of() : buildRecentActivities(userId))
                .build();

        CurriculumGenerateResponse aiResponse = callFastApi(aiRequest);

        int duration = calculateDuration(aiResponse.getNodes());

        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String previewKey = "curriculumPreview:" + userId + ":" + uuid;
        savePreviewToRedis(previewKey, aiResponse);

        log.info("커리큘럼 미리보기 생성 완료. userId={}, previewKey={}", userId, previewKey);

        return CurriculumPreviewResponse.builder()
                .curriculumPreviewKey(previewKey)
                .duration(duration)
                .recommendationReason(aiResponse.getRecommendationReason())
                .techStacks(aiResponse.getTechStacks())
                .nodes(aiResponse.getNodes())
                .optionA(aiResponse.getOptionA())
                .optionB(aiResponse.getOptionB())
                .build();
    }

    @Override
    @Transactional
    public CurriculumConfirmResponse confirm(Long userId, CurriculumConfirmRequest request) {
        String previewKey = request.getCurriculumPreviewKey();

        String json = redisService.get(previewKey);
        if (json == null) {
            throw new IllegalArgumentException("만료되었거나 존재하지 않는 미리보기 키입니다. key=" + previewKey);
        }

        CurriculumGenerateResponse aiResponse;
        try {
            aiResponse = objectMapper.readValue(json, CurriculumGenerateResponse.class);
        } catch (JacksonException e) {
            throw new IllegalStateException("커리큘럼 미리보기 데이터 역직렬화 실패", e);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다. userId=" + userId));

        CurriculumGenerateResponse selectedPreview = resolveSelectedPreview(aiResponse, request.getSelectedOptionType());
        int duration = calculateDuration(selectedPreview.getNodes());

        Curriculum curriculum = Curriculum.builder()
                .user(user)
                .status(CurriculumStatus.ACTIVE)
                .duration(duration)
                .build();
        curriculumRepository.save(curriculum);
        saveCurriculumTechStacks(curriculum, selectedPreview.getTechStacks());

        PreviewReasonDto reason = selectedPreview.getRecommendationReason();
        if (reason != null) {
            curriculumRecommendationReasonRepository.save(CurriculumRecommendationReason.builder()
                    .curriculum(curriculum)
                    .summaryLine(reason.getSummaryLine())
                    .userContext(reason.getUserContext())
                    .aiInterpretation(reason.getAiInterpretation())
                    .curriculumRationale(reason.getCurriculumRationale())
                    .build());
        }

        Calendar calendarClient = buildGoogleCalendarClientOrNull(userId);

        List<ConfirmNodeDto> confirmNodes = new ArrayList<>();
        for (PreviewNodeDto nodeDto : selectedPreview.getNodes()) {
            CurriculumNode node = CurriculumNode.builder()
                    .curriculum(curriculum)
                    .title(nodeDto.getTitle())
                    .description(nodeDto.getDescription())
                    .scheduledDate(nodeDto.getScheduledDate())
                    .expectedMinutes(nodeDto.getExpectedMinutes() != null ? nodeDto.getExpectedMinutes() : 0)
                    .build();
            curriculumNodeRepository.save(node);

            CurriculumNodeCalendarSync sync = CurriculumNodeCalendarSync.builder()
                    .curriculumNode(node)
                    .build();

            if (calendarClient != null) {
                try {
                    Event event = googleCalendarClientService.createAllDayEvent(
                            calendarClient,
                            node.getTitle(),
                            node.getDescription(),
                            node.getScheduledDate()
                    );
                    sync.synced("primary", event.getId(), event.getEtag());
                } catch (IOException e) {
                    log.warn("커리큘럼 노드 Google Calendar 이벤트 생성 실패. nodeId={}", node.getCurriculumNodeId(), e);
                    sync.syncFailed();
                }
            }

            curriculumNodeCalendarSyncRepository.save(sync);

            confirmNodes.add(ConfirmNodeDto.builder()
                    .curriculumNodeId(node.getCurriculumNodeId())
                    .title(node.getTitle())
                    .scheduledDate(node.getScheduledDate())
                    .expectedMinutes(node.getExpectedMinutes())
                    .progressStatus(node.getProgressStatus())
                    .calendarSync(CalendarSyncDto.builder()
                            .syncStatus(sync.getSyncStatus())
                            .googleEventId(sync.getGoogleEventId())
                            .build())
                    .build());
        }

        redisService.delete(previewKey);
        invalidateCalendarCacheForNodes(userId, selectedPreview.getNodes());

        log.info("커리큘럼 확정 저장 완료. userId={}, curriculumId={}", userId, curriculum.getCurriculumId());

        return CurriculumConfirmResponse.builder()
                .curriculumId(curriculum.getCurriculumId())
                .status(curriculum.getStatus())
                .createdAt(curriculum.getCreatedAt())
                .techStacks(selectedPreview.getTechStacks())
                .nodes(confirmNodes)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public CurriculumReasonResponse getReason(Long userId, Long curriculumId) {
        Curriculum curriculum = curriculumRepository.findById(curriculumId)
                .orElseThrow(() -> new CurriculumNotFoundException(curriculumId));

        if (!curriculum.getUser().getUserId().equals(userId)) {
            throw new CurriculumAccessDeniedException(curriculumId);
        }

        CurriculumRecommendationReason reason = curriculumRecommendationReasonRepository
                .findByCurriculumCurriculumId(curriculumId)
                .orElseThrow(() -> new CurriculumNotFoundException(curriculumId));

        log.info("커리큘럼 AI 추천 근거 조회. userId={}, curriculumId={}", userId, curriculumId);

        return CurriculumReasonResponse.from(reason);
    }

    @Override
    @Transactional(readOnly = true)
    public CurriculumNodeResponse getNode(Long userId, Long curriculumNodeId) {
        CurriculumNode node = curriculumNodeRepository.findById(curriculumNodeId)
                .orElseThrow(() -> new CurriculumNodeNotFoundException(curriculumNodeId));

        if (!node.getCurriculum().getUser().getUserId().equals(userId)) {
            throw new CurriculumNodeAccessDeniedException(curriculumNodeId);
        }

        log.info("커리큘럼 노드 상세 조회. userId={}, curriculumNodeId={}", userId, curriculumNodeId);

        return CurriculumNodeResponse.from(node);
    }

    @Override
    @Transactional
    public CurriculumNodeResponse updateNode(Long userId, Long nodeId, CurriculumNodeUpdateRequest request) {
        if (request.hasNoFields()) {
            throw new IllegalArgumentException("수정할 필드가 없습니다.");
        }

        CurriculumNode node = curriculumNodeRepository.findById(nodeId)
                .orElseThrow(() -> new CurriculumNodeNotFoundException(nodeId));

        if (!node.getCurriculum().getUser().getUserId().equals(userId)) {
            throw new CurriculumNodeAccessDeniedException(nodeId);
        }

        LocalDate oldDate = node.getScheduledDate();

        node.update(request.getTitle(), request.getDescription(), request.getScheduledDate(), request.getProgressStatus());

        curriculumNodeCalendarSyncRepository.findByCurriculumNodeCurriculumNodeId(nodeId)
                .ifPresent(CurriculumNodeCalendarSync::markNotSynced);

        invalidateCalendarCacheForMonths(userId, oldDate, node.getScheduledDate());

        log.info("커리큘럼 노드 수정 완료. userId={}, nodeId={}", userId, nodeId);

        return CurriculumNodeResponse.from(node);
    }

    private Calendar buildGoogleCalendarClientOrNull(Long userId) {
        try {
            OAuthAccount oAuthAccount = oAuthAccountRepository
                    .findByUserUserIdAndProvider(userId, OAuthProvider.GOOGLE)
                    .orElse(null);
            if (oAuthAccount == null || oAuthAccount.getRefreshToken() == null) {
                return null;
            }
            String refreshToken = oAuthTokenCryptoService.decrypt(oAuthAccount.getRefreshToken());
            return googleCalendarClientService.buildCalendarClient(refreshToken);
        } catch (Exception e) {
            log.warn("Google Calendar 클라이언트 생성 실패, 캘린더 연동 없이 진행. userId={}", userId, e);
            return null;
        }
    }

    private void invalidateCalendarCacheForNodes(Long userId, List<PreviewNodeDto> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return;
        }
        nodes.stream()
                .map(PreviewNodeDto::getScheduledDate)
                .map(YearMonth::from)
                .distinct()
                .forEach(ym -> redisService.delete(
                        "calendar:" + userId + ":" + ym.getYear() + ":" + ym.getMonthValue()));
    }

    private void invalidateCalendarCacheForMonths(Long userId, LocalDate date1, LocalDate date2) {
        YearMonth ym1 = YearMonth.from(date1);
        YearMonth ym2 = YearMonth.from(date2);
        redisService.delete("calendar:" + userId + ":" + ym1.getYear() + ":" + ym1.getMonthValue());
        if (!ym1.equals(ym2)) {
            redisService.delete("calendar:" + userId + ":" + ym2.getYear() + ":" + ym2.getMonthValue());
        }
    }

    private List<GoogleCalendarEventDto> fetchGoogleCalendarEvents(Long userId) {
        try {
            OAuthAccount oAuthAccount = oAuthAccountRepository
                    .findByUserUserIdAndProvider(userId, OAuthProvider.GOOGLE)
                    .orElse(null);
            if (oAuthAccount == null || oAuthAccount.getRefreshToken() == null) {
                return List.of();
            }
            String refreshToken = oAuthTokenCryptoService.decrypt(oAuthAccount.getRefreshToken());
            Calendar client = googleCalendarClientService.buildCalendarClient(refreshToken);
            List<Event> events = googleCalendarClientService.listAllEvents(client);
            return events.stream()
                    .filter(e -> e.getSummary() != null)
                    .map(e -> GoogleCalendarEventDto.builder()
                            .title(e.getSummary())
                            .startDate(googleCalendarClientService.parseEventStartDate(e).toString())
                            .endDate(googleCalendarClientService.parseEventEndDate(e).toString())
                            .build())
                    .toList();
        } catch (IOException | IllegalStateException e) {
            log.warn("Google Calendar 이벤트 조회 실패, 빈 목록으로 진행. userId={}", userId, e);
            return List.of();
        }
    }

    private List<SkillStatDto> buildUserTechStacks(Long userId) {
        List<Object[]> rows = activityHistoryTechStackRepository
                .findTechStackCountsByUserId(userId, PageRequest.of(0, 10));
        return rows.stream()
                .map(row -> SkillStatDto.builder()
                        .skill(((TechStack) row[0]).getTechName())
                        .count(((Long) row[1]).intValue())
                .build())
                .toList();
    }

    private List<RecentActivityDto> buildRecentActivities(Long userId) {
        return activityHistoryRepository.findTop10ByUserUserIdAndIsIncludedTrueOrderByActivityDateDesc(userId).stream()
                .map(activity -> RecentActivityDto.builder()
                        .activityType(mapActivityType(activity.getActivityType()))
                        .category(activity.getCategory() == null ? null : activity.getCategory().name())
                        .title(activity.getTitle())
                        .description(activity.getDescription())
                        .activityDate(activity.getActivityDate())
                        .techStacks(activityHistoryTechStackRepository
                                .findByActivityHistoryActivityHistoryId(activity.getActivityHistoryId()).stream()
                                .map(techStack -> techStack.getTechStack().getTechName())
                                .toList())
                        .build())
                .toList();
    }

    private String mapActivityType(ActivityType activityType) {
        if (activityType == ActivityType.GITHUB_COMMIT) {
            return "COMMIT";
        }
        return activityType.name();
    }

    CurriculumGenerateResponse callFastApi(CurriculumGenerateRequest request) {
        try {
            CurriculumGenerateResponse response = aiRestClient.buildAiRestClient()
                    .post()
                    .uri(aiServerUrl + curriculumGeneratePath)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(CurriculumGenerateResponse.class);
            if (response == null) {
                throw new IllegalStateException("FastAPI 커리큘럼 생성 응답이 null입니다.");
            }
            return response;
        } catch (RestClientException e) {
            throw new IllegalStateException("FastAPI 커리큘럼 생성 요청 실패", e);
        }
    }

    private int calculateDuration(List<PreviewNodeDto> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return 0;
        }
        LocalDate minDate = nodes.stream()
                .map(PreviewNodeDto::getScheduledDate)
                .min(Comparator.naturalOrder())
                .orElse(LocalDate.now());
        LocalDate maxDate = nodes.stream()
                .map(PreviewNodeDto::getScheduledDate)
                .max(Comparator.naturalOrder())
                .orElse(LocalDate.now());
        return (int) (maxDate.toEpochDay() - minDate.toEpochDay() + 1);
    }

    private CurriculumGenerateResponse resolveSelectedPreview(
            CurriculumGenerateResponse response,
            String selectedOptionType
    ) {
        if (response == null) {
            return null;
        }

        PreviewOptionDto selectedOption = null;
        if ("STRENGTH".equalsIgnoreCase(selectedOptionType)) {
            selectedOption = response.getOptionB();
        } else if ("WEAKNESS".equalsIgnoreCase(selectedOptionType)) {
            selectedOption = response.getOptionA();
        }

        if (selectedOption == null) {
            return response;
        }

        return CurriculumGenerateResponse.builder()
                .recommendationReason(selectedOption.getRecommendationReason())
                .techStacks(selectedOption.getTechStacks())
                .nodes(selectedOption.getNodes())
                .optionA(response.getOptionA())
                .optionB(response.getOptionB())
                .build();
    }

    private void savePreviewToRedis(String previewKey, CurriculumGenerateResponse data) {
        try {
            String json = objectMapper.writeValueAsString(data);
            redisService.save(previewKey, json, 30L, TimeUnit.MINUTES);
        } catch (JacksonException e) {
            throw new IllegalStateException("커리큘럼 미리보기 Redis 저장 실패", e);
        }
    }

    private void saveCurriculumTechStacks(Curriculum curriculum, List<String> techStacks) {
        if (techStacks == null || techStacks.isEmpty()) {
            return;
        }

        List<CurriculumTechStack> links = techStacks.stream()
                .filter(java.util.Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .map(techStackRepository::findByTechNameIgnoreCase)
                .flatMap(java.util.Optional::stream)
                .map(techStack -> CurriculumTechStack.builder()
                        .curriculum(curriculum)
                        .techStack(techStack)
                        .build())
                .toList();

        if (!links.isEmpty()) {
            curriculumTechStackRepository.saveAll(links);
        }
    }
}
