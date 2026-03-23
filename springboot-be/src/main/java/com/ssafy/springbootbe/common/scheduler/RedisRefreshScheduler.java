package com.ssafy.springbootbe.common.scheduler;

import com.ssafy.springbootbe.domain.recommendations.service.RecommendationsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisRefreshScheduler {
    private final RecommendationsService recommendationsService;

    @Scheduled(cron = "0 1 18 * * *", zone = "Asia/Seoul")
    public void refresh() {
        log.info("일일 추천 배치 시작");
        recommendationsService.refreshDailyRecommendations();
        log.info("일일 추천 배치 종료");
    }
}
