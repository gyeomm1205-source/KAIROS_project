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
import com.ssafy.springbootbe.persistence.user.entity.User;
import com.ssafy.springbootbe.persistence.user.entity.UserTechStack;
import com.ssafy.springbootbe.persistence.user.entity.UserTechStackBaseline;
import com.ssafy.springbootbe.persistence.user.repository.UserTechStackBaselineRepository;
import com.ssafy.springbootbe.persistence.user.repository.UserRepository;
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
    private static final double RECENT_GROWTH_BASELINE_FLOOR = 1.0;
    private static final double RECENT_GROWTH_DELTA_WEIGHT = 0.7;
    private static final double RECENT_GROWTH_RELATIVE_WEIGHT = 0.3;

    private final ActivityHistoryRepository activityHistoryRepository;
    private final ActivityHistoryTechStackRepository activityHistoryTechStackRepository;
    private final UserRepository userRepository;
    private final UserTechStackRepository userTechStackRepository;
    private final UserTechStackBaselineRepository userTechStackBaselineRepository;
    private final LearningStateService learningStateService;

    @Override
    @Transactional(readOnly = true)
    public GrowthReportResponse getGrowthReport(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        Pageable topTechPageable = PageRequest.of(0, TOP_TECH_STACKS_LIMIT);
        Pageable rankingPageable = PageRequest.of(0, TECH_ACTIVITY_RANKING_LIMIT);
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

        List<LocalDateTime> activityDates = activityHistoryRepository.findActivityDatesByUserId(userId);
        int maxStreakDays = calculateMaxStreakDays(activityDates);

        List<UserTechStack> topScoreStacks = userTechStackRepository
                .findTop6ByUserUserIdOrderByScoreDesc(userId);
        Map<Long, Double> baselineScoreByTechStackId = userTechStackBaselineRepository.findByUserUserId(userId).stream()
                .collect(Collectors.toMap(
                        baseline -> baseline.getTechStack().getTechStackId(),
                        baseline -> baseline.getBaselineScore() == null ? 0.0 : baseline.getBaselineScore(),
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
        List<GrowthReportResponse.TechStackScoreInfo> techScoreSnapshot = topScoreStacks.stream()
                .map(uts -> GrowthReportResponse.TechStackScoreInfo.builder()
                        .techStackId(uts.getTechStack().getTechStackId())
                        .techName(uts.getTechStack().getTechName())
                        .iconUrl(uts.getTechStack().getIconUrl())
                        .color(uts.getTechStack().getColor())
                        .score(uts.getScore())
                        .build())
                .collect(Collectors.toList());
        List<GrowthReportResponse.TechStackGrowthInfo> techGrowthComparisons =
                buildTechGrowthComparisons(topScoreStacks, baselineScoreByTechStackId);
        GrowthReportResponse.TechStackGrowthInfo recentGrowth = techGrowthComparisons.stream()
                .max(Comparator.comparing(this::calculateRecentGrowthScore))
                .orElse(null);
        GrowthReportResponse.TechStackInfo recentGrowthTech = recentGrowth == null ? null : GrowthReportResponse.TechStackInfo.builder()
                .techStackId(recentGrowth.getTechStackId())
                .techName(recentGrowth.getTechName())
                .iconUrl(recentGrowth.getIconUrl())
                .color(recentGrowth.getColor())
                .build();
        Double recentGrowthDelta = recentGrowth == null ? null : recentGrowth.getGrowthDelta();
        GrowthReportResponse.TechStackPercentileInfo topSkillPercentile = buildTopSkillPercentile(topScoreStacks);

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
                .userCreatedAt(user.getCreatedAt())
                .totalActivityCount(totalActivityCount)
                .recentGrowthTech(recentGrowthTech)
                .recentGrowthDelta(recentGrowthDelta)
                .maxStreakDays(maxStreakDays)
                .techScoreSnapshot(techScoreSnapshot)
                .techGrowthComparisons(techGrowthComparisons)
                .topSkillPercentile(topSkillPercentile)
                .monthlyActivityCounts(monthlyActivityCounts)
                .techActivityRanking(techActivityRanking)
                .build();
    }

    private List<GrowthReportResponse.TechStackGrowthInfo> buildTechGrowthComparisons(
            List<UserTechStack> currentStacks,
            Map<Long, Double> baselineScoreByTechStackId
    ) {
        return currentStacks.stream()
                .filter(userTechStack -> userTechStack.getScore() != null)
                .map(userTechStack -> {
                    TechStack techStack = userTechStack.getTechStack();
                    double baselineScore = baselineScoreByTechStackId.getOrDefault(techStack.getTechStackId(), 0.0);
                    double currentScore = userTechStack.getScore() == null ? 0.0 : userTechStack.getScore();
                    return GrowthReportResponse.TechStackGrowthInfo.builder()
                            .techStackId(techStack.getTechStackId())
                            .techName(techStack.getTechName())
                            .iconUrl(techStack.getIconUrl())
                            .color(techStack.getColor())
                            .baselineScore(roundToTwoDecimals(baselineScore))
                            .currentScore(roundToTwoDecimals(currentScore))
                            .growthDelta(roundToTwoDecimals(currentScore - baselineScore))
                            .build();
                })
                .toList();
    }

    private GrowthReportResponse.TechStackPercentileInfo buildTopSkillPercentile(List<UserTechStack> topScoreStacks) {
        if (topScoreStacks == null || topScoreStacks.isEmpty()) {
            return null;
        }

        UserTechStack topSkill = topScoreStacks.getFirst();
        TechStack techStack = topSkill.getTechStack();
        Double score = topSkill.getScore() == null ? 0.0 : topSkill.getScore();
        long comparedUserCount = userTechStackRepository.countByTechStackTechStackId(techStack.getTechStackId());
        if (comparedUserCount <= 0) {
            return null;
        }

        long higherScoreCount = userTechStackRepository.countByTechStackTechStackIdAndScoreGreaterThan(
                techStack.getTechStackId(),
                score
        );
        double topPercentile = roundToTwoDecimals(((double) (higherScoreCount + 1) / comparedUserCount) * 100.0);

        return GrowthReportResponse.TechStackPercentileInfo.builder()
                .techStackId(techStack.getTechStackId())
                .techName(techStack.getTechName())
                .iconUrl(techStack.getIconUrl())
                .color(techStack.getColor())
                .score(roundToTwoDecimals(score))
                .topPercentile(topPercentile)
                .comparedUserCount(comparedUserCount)
                .build();
    }

    private double calculateRecentGrowthScore(GrowthReportResponse.TechStackGrowthInfo growthInfo) {
        if (growthInfo == null) {
            return 0.0;
        }

        double baselineScore = growthInfo.getBaselineScore() == null ? 0.0 : growthInfo.getBaselineScore();
        double delta = growthInfo.getGrowthDelta() == null ? 0.0 : growthInfo.getGrowthDelta();
        double relativeGrowth = delta / Math.max(baselineScore, RECENT_GROWTH_BASELINE_FLOOR);

        return (delta * RECENT_GROWTH_DELTA_WEIGHT) + (relativeGrowth * RECENT_GROWTH_RELATIVE_WEIGHT);
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

    private double roundToTwoDecimals(double value) {
        return Math.round(value * 100.0) / 100.0;
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
        learningStateService.recalculateForUser(userId);

        log.info("활동 이력 포함 여부 수정. userId={}, activityHistoryId={}, isIncluded={}", userId, activityHistoryId, request.getIsIncluded());

        return ActivityInclusionResponse.builder()
                .activityHistoryId(activity.getActivityHistoryId())
                .isIncluded(activity.getIsIncluded())
                .build();
    }
}
