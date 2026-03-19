package com.ssafy.springbootbe.common.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RedisRefreshScheduler {
    int i = 1;

    @Scheduled(cron = "*/1 * * * * *", zone = "Asia/Seoul")
    public void refresh() {
        log.info("scheduler test: "+i++);
    }
}
