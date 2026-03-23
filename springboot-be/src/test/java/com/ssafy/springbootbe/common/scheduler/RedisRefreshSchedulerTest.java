package com.ssafy.springbootbe.common.scheduler;

import com.ssafy.springbootbe.domain.recommendations.service.RecommendationsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RedisRefreshSchedulerTest {

    @Mock
    private RecommendationsService recommendationsService;

    @InjectMocks
    private RedisRefreshScheduler redisRefreshScheduler;

    @Test
    void refresh_추천_배치_서비스를_호출한다() {
        // when
        redisRefreshScheduler.refresh();

        // then
        verify(recommendationsService).refreshDailyRecommendations();
    }
}
