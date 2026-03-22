package com.ssafy.springbootbe.domain.activities.service;

import com.ssafy.springbootbe.domain.activities.dto.request.ActivityInclusionRequest;
import com.ssafy.springbootbe.domain.activities.dto.response.ActivityHistoryResponse;
import com.ssafy.springbootbe.domain.activities.dto.response.ActivityInclusionResponse;
import com.ssafy.springbootbe.domain.activities.dto.response.ActivityPageResponse;
import com.ssafy.springbootbe.domain.activities.dto.response.GrowthReportResponse;
import com.ssafy.springbootbe.domain.activities.exception.ActivityAccessDeniedException;
import com.ssafy.springbootbe.domain.activities.exception.ActivityNotFoundException;
import com.ssafy.springbootbe.common.dto.TechStackInfo;
import com.ssafy.springbootbe.persistence.activity.entity.ActivityHistory;
import com.ssafy.springbootbe.persistence.activity.repository.ActivityHistoryRepository;
import com.ssafy.springbootbe.persistence.activity.repository.ActivityHistoryTechStackRepository;
import com.ssafy.springbootbe.persistence.activity.type.ActivityType;
import com.ssafy.springbootbe.persistence.techstack.entity.TechStack;
import com.ssafy.springbootbe.persistence.user.entity.UserTechStack;
import com.ssafy.springbootbe.persistence.user.repository.UserTechStackRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActivitiesServiceImpl implements ActivitiesService {

    private static final int TOP_TECH_STACKS_LIMIT = 10;
    private static final int TECH_ACTIVITY_RANKING_LIMIT = 5;
    private static final int RECENT_GROWTH_DAYS = 30;

    private final ActivityHistoryRepository activityHistoryRepository;
    private final ActivityHistoryTechStackRepository activityHistoryTechStackRepository;
    private final UserTechStackRepository userTechStackRepository;

    @Override
    @Transactional(readOnly = true)
    public GrowthReportResponse getGrowthReport(Long userId) {
        Pageable topTechPageable = PageRequest.of(0, TOP_TECH_STACKS_LIMIT);
        Pageable rankingPageable = PageRequest.of(0, TECH_ACTIVITY_RANKING_LIMIT);
        Pageable recentGrowthPageable = PageRequest.of(0, 1);
        LocalDateTime since = LocalDateTime.now().minusDays(RECENT_GROWTH_DAYS);

        List<Object[]> topTechRaws = activityHistoryTechStackRepository
                .findTechStackCountsByUserId(userId, topTechPageable);
        List<GrowthReportResponse.TechStackCountInfo> topTechStacks = topTechRaws.stream()
                .map(row -> {
                    TechStack ts = (TechStack) row[0];
                    return GrowthReportResponse.TechStackCountInfo.builder()
                            .techStackId(ts.getTechStackId())
                            .techName(ts.getTechName())
                            .iconUrl(ts.getIconUrl())
                            .color(ts.getColor())
                            .count((Long) row[1])
                            .build();
                }).collect(Collectors.toList());

        long totalActivityCount = activityHistoryRepository.countByUserUserId(userId);

        List<Object[]> recentGrowthRaws = activityHistoryTechStackRepository
                .findTechStackCountsSince(userId, since, recentGrowthPageable);
        GrowthReportResponse.TechStackInfo recentGrowthTech = null;
        if (!recentGrowthRaws.isEmpty()) {
            TechStack ts = (TechStack) recentGrowthRaws.get(0)[0];
            recentGrowthTech = GrowthReportResponse.TechStackInfo.builder()
                    .techStackId(ts.getTechStackId())
                    .techName(ts.getTechName())
                    .iconUrl(ts.getIconUrl())
                    .color(ts.getColor())
                    .build();
        }

        List<LocalDateTime> activityDates = activityHistoryRepository.findActivityDatesByUserId(userId);
        int maxStreakDays = calculateMaxStreakDays(activityDates);

        List<UserTechStack> topScoreStacks = userTechStackRepository
                .findTop6ByUserUserIdOrderByScoreDesc(userId);
        List<GrowthReportResponse.TechStackScoreInfo> techScoreSnapshot = topScoreStacks.stream()
                .map(uts -> GrowthReportResponse.TechStackScoreInfo.builder()
                        .techStackId(uts.getTechStack().getTechStackId())
                        .techName(uts.getTechStack().getTechName())
                        .iconUrl(uts.getTechStack().getIconUrl())
                        .color(uts.getTechStack().getColor())
                        .score(uts.getScore())
                        .build())
                .collect(Collectors.toList());

        List<Object[]> monthlyRaws = activityHistoryRepository.findMonthlyActivityCountsByUserId(userId);
        List<GrowthReportResponse.MonthlyActivityCountInfo> monthlyActivityCounts = buildMonthlyActivityCounts(monthlyRaws);

        List<Object[]> rankingRaws = activityHistoryTechStackRepository
                .findTechStackCountsByUserId(userId, rankingPageable);
        List<GrowthReportResponse.TechStackCountInfo> techActivityRanking = rankingRaws.stream()
                .map(row -> {
                    TechStack ts = (TechStack) row[0];
                    return GrowthReportResponse.TechStackCountInfo.builder()
                            .techStackId(ts.getTechStackId())
                            .techName(ts.getTechName())
                            .iconUrl(ts.getIconUrl())
                            .color(ts.getColor())
                            .count((Long) row[1])
                            .build();
                }).collect(Collectors.toList());

        log.info("성장 일지 조회. userId={}, totalActivityCount={}", userId, totalActivityCount);

        return GrowthReportResponse.builder()
                .topTechStacks(topTechStacks)
                .totalActivityCount(totalActivityCount)
                .recentGrowthTech(recentGrowthTech)
                .maxStreakDays(maxStreakDays)
                .techScoreSnapshot(techScoreSnapshot)
                .monthlyActivityCounts(monthlyActivityCounts)
                .techActivityRanking(techActivityRanking)
                .build();
    }

    private int calculateMaxStreakDays(List<LocalDateTime> activityDates) {
        if (activityDates.isEmpty()) {
            return 0;
        }
        Set<LocalDate> distinctDates = activityDates.stream()
                .map(LocalDateTime::toLocalDate)
                .collect(Collectors.toCollection(TreeSet::new));

        List<LocalDate> sortedDates = new ArrayList<>(distinctDates);
        int maxStreak = 1;
        int currentStreak = 1;
        for (int i = 1; i < sortedDates.size(); i++) {
            if (sortedDates.get(i).equals(sortedDates.get(i - 1).plusDays(1))) {
                currentStreak++;
                maxStreak = Math.max(maxStreak, currentStreak);
            } else {
                currentStreak = 1;
            }
        }
        return maxStreak;
    }

    private List<GrowthReportResponse.MonthlyActivityCountInfo> buildMonthlyActivityCounts(List<Object[]> rawRows) {
        Map<String, Map<ActivityType, Long>> grouped = new LinkedHashMap<>();
        for (Object[] row : rawRows) {
            int year = ((Number) row[0]).intValue();
            int month = ((Number) row[1]).intValue();
            ActivityType activityType = (ActivityType) row[2];
            long count = ((Number) row[3]).longValue();
            String key = year + "-" + month;
            grouped.computeIfAbsent(key, k -> new LinkedHashMap<>()).put(activityType, count);
        }
        return grouped.entrySet().stream()
                .map(entry -> {
                    String[] parts = entry.getKey().split("-");
                    return GrowthReportResponse.MonthlyActivityCountInfo.builder()
                            .year(Integer.parseInt(parts[0]))
                            .month(Integer.parseInt(parts[1]))
                            .counts(entry.getValue())
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ActivityPageResponse findActivities(Long userId, Integer year, Integer month, String sort, int page, int size) {
        Sort.Direction direction = "oldest".equalsIgnoreCase(sort) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(direction, "activityDate"));

        Page<ActivityHistory> activityPage;
        if (year != null && month != null) {
            activityPage = activityHistoryRepository.findByUserUserIdAndYearAndMonth(userId, year, month, pageable);
        } else if (year != null) {
            activityPage = activityHistoryRepository.findByUserUserIdAndYear(userId, year, pageable);
        } else {
            activityPage = activityHistoryRepository.findByUserUserId(userId, pageable);
        }

        List<ActivityHistoryResponse> items = activityPage.getContent().stream()
                .map(activity -> {
                    List<TechStackInfo> techStacks = activityHistoryTechStackRepository
                            .findByActivityHistoryActivityHistoryId(activity.getActivityHistoryId())
                            .stream()
                            .map(ahts -> TechStackInfo.from(ahts.getTechStack()))
                            .toList();
                    return ActivityHistoryResponse.from(activity, techStacks);
                })
                .toList();

        log.info("활동 이력 조회. userId={}, year={}, month={}, total={}", userId, year, month, activityPage.getTotalElements());

        return ActivityPageResponse.builder()
                .total(activityPage.getTotalElements())
                .page(page)
                .size(size)
                .items(items)
                .build();
    }

    @Override
    @Transactional
    public ActivityInclusionResponse updateInclusion(Long userId, Long activityHistoryId, ActivityInclusionRequest request) {
        ActivityHistory activity = activityHistoryRepository.findById(activityHistoryId)
                .orElseThrow(() -> new ActivityNotFoundException(activityHistoryId));

        if (!activity.getUser().getUserId().equals(userId)) {
            throw new ActivityAccessDeniedException(activityHistoryId);
        }

        activity.toggleInclusion(request.getIsIncluded());

        log.info("활동 이력 포함 여부 수정. userId={}, activityHistoryId={}, isIncluded={}", userId, activityHistoryId, request.getIsIncluded());

        return ActivityInclusionResponse.builder()
                .activityHistoryId(activity.getActivityHistoryId())
                .isIncluded(activity.getIsIncluded())
                .build();
    }
}
