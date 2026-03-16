package com.ssafy.springbootbe.domain.test;

import com.ssafy.springbootbe.common.redis.RedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class TestService {
    private final DumyTableRepository dumyTableRepository;
    private final RedisService redisService;

    @Value("${service.refresh-token-duration}")
    private long duration;

    public Map<String, Object> tstGet() {
        return Map.of("key", "제목", "value", "내용", "duration", duration);
    }

    public String tstPost() {
        DumyTable dummyTable = DumyTable.builder()
                .dumyName("윰재")
                .build();
        dummyTable = dumyTableRepository.save(dummyTable);
        redisService.save("dumy:"+dummyTable.getDumyId(), "YoomCache", 30L, TimeUnit.SECONDS);
        return "API 성공!";
    }
}
