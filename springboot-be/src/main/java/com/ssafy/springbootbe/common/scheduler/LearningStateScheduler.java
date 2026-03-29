package com.ssafy.springbootbe.common.scheduler;

import com.ssafy.springbootbe.domain.activities.service.LearningStateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LearningStateScheduler {

    private final LearningStateService learningStateService;

    @Scheduled(cron = "0 0 0 1 * *", zone = "Asia/Seoul")
    public void refreshMonthlyLearningState() {
        log.info("월간 learning state 재계산 시작");
        learningStateService.recalculateForAllUsers();
        log.info("월간 learning state 재계산 종료");
    }
}
