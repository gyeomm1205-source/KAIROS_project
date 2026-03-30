package com.ssafy.springbootbe.domain.activities.service;

import com.ssafy.springbootbe.persistence.activity.entity.ActivityHistory;
import com.ssafy.springbootbe.persistence.activity.entity.ActivityHistoryTechStack;
import com.ssafy.springbootbe.persistence.activity.repository.ActivityHistoryTechStackRepository;
import com.ssafy.springbootbe.persistence.activity.type.ActivityType;
import com.ssafy.springbootbe.persistence.techstack.entity.TechStack;
import com.ssafy.springbootbe.persistence.user.entity.User;
import com.ssafy.springbootbe.persistence.user.entity.UserTechStack;
import com.ssafy.springbootbe.persistence.user.repository.UserRepository;
import com.ssafy.springbootbe.persistence.user.repository.UserTechStackRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class LearningStateServiceImpl implements LearningStateService {

    private static final Map<ActivityType, Double> ACTIVITY_WEIGHTS = Map.of(
            ActivityType.VELOG_POST, 1.0,
            ActivityType.GITHUB_COMMIT, 0.1,
            ActivityType.GITHUB_PR, 0.2,
            ActivityType.QUIZ, 0.1,
            ActivityType.REFERENCE, 0.3
    );

    private final UserRepository userRepository;
    private final UserTechStackRepository userTechStackRepository;
    private final ActivityHistoryTechStackRepository activityHistoryTechStackRepository;

    @Override
    @Transactional
    public void recalculateForUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 userId=" + userId));

        List<ActivityHistoryTechStack> includedLinks = activityHistoryTechStackRepository.findIncludedByUserId(userId);
        Map<Long, Double> scoreByTechStackId = new LinkedHashMap<>();
        Map<Long, TechStack> techStackById = new HashMap<>();

        for (ActivityHistoryTechStack link : includedLinks) {
            ActivityHistory activityHistory = link.getActivityHistory();
            TechStack techStack = link.getTechStack();
            double activityWeight = ACTIVITY_WEIGHTS.getOrDefault(activityHistory.getActivityType(), 0.0);
            double timeDecayWeight = resolveTimeDecayWeight(activityHistory.getActivityDate());

            scoreByTechStackId.merge(
                    techStack.getTechStackId(),
                    roundScore(activityWeight * timeDecayWeight),
                    Double::sum
            );
            techStackById.put(techStack.getTechStackId(), techStack);
        }

        List<UserTechStack> existingUserTechStacks = userTechStackRepository.findByUserUserId(userId);
        Map<Long, UserTechStack> existingByTechStackId = new HashMap<>();
        for (UserTechStack userTechStack : existingUserTechStacks) {
            Long techStackId = userTechStack.getTechStack().getTechStackId();
            existingByTechStackId.put(techStackId, userTechStack);
            techStackById.putIfAbsent(techStackId, userTechStack.getTechStack());
        }

        for (Map.Entry<Long, TechStack> entry : techStackById.entrySet()) {
            Long techStackId = entry.getKey();
            Double score = roundScore(scoreByTechStackId.getOrDefault(techStackId, 0.0));
            UserTechStack userTechStack = existingByTechStackId.get(techStackId);

            if (userTechStack != null) {
                userTechStack.updateScore(score);
                continue;
            }

            userTechStackRepository.save(UserTechStack.builder()
                    .user(user)
                    .techStack(entry.getValue())
                    .score(score)
                    .build());
        }

        log.info("사용자 learning state 재계산 완료. userId={}, techStackCount={}", userId, techStackById.size());
    }

    @Override
    @Transactional
    public void recalculateForAllUsers() {
        userRepository.findAll().forEach(user -> recalculateForUser(user.getUserId()));
    }

    private double resolveTimeDecayWeight(LocalDateTime activityDate) {
        if (activityDate == null) {
            return 0.5;
        }

        long monthsAgo = ChronoUnit.MONTHS.between(activityDate.toLocalDate().withDayOfMonth(1), LocalDate.now().withDayOfMonth(1));
        if (monthsAgo <= 3) {
            return 1.0;
        }
        if (monthsAgo <= 5) {
            return 0.9;
        }
        if (monthsAgo <= 7) {
            return 0.8;
        }
        if (monthsAgo <= 9) {
            return 0.7;
        }
        if (monthsAgo <= 11) {
            return 0.6;
        }
        return 0.5;
    }

    private double roundScore(double score) {
        return Math.round(score * 100.0) / 100.0;
    }
}
