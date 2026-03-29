package com.ssafy.springbootbe.domain.activities.service;

import com.ssafy.springbootbe.persistence.user.entity.UserTechStack;
import com.ssafy.springbootbe.persistence.user.entity.UserTechStackBaseline;
import com.ssafy.springbootbe.persistence.user.repository.UserTechStackBaselineRepository;
import com.ssafy.springbootbe.persistence.user.repository.UserTechStackRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LearningStateBaselineServiceImpl implements LearningStateBaselineService {

    private final UserTechStackRepository userTechStackRepository;
    private final UserTechStackBaselineRepository userTechStackBaselineRepository;

    @Override
    @Transactional
    public void captureInitialBaselineIfAbsent(Long userId) {
        if (userTechStackBaselineRepository.existsByUserUserId(userId)) {
            return;
        }

        List<UserTechStack> currentStacks = userTechStackRepository.findByUserUserId(userId);
        if (currentStacks.isEmpty()) {
            return;
        }

        List<UserTechStackBaseline> baselines = currentStacks.stream()
                .map(userTechStack -> UserTechStackBaseline.builder()
                        .user(userTechStack.getUser())
                        .techStack(userTechStack.getTechStack())
                        .baselineScore(userTechStack.getScore() == null ? 0.0 : userTechStack.getScore())
                        .build())
                .toList();

        userTechStackBaselineRepository.saveAll(baselines);
        log.info("초기 learning state baseline 저장 완료. userId={}, techStackCount={}", userId, baselines.size());
    }
}
