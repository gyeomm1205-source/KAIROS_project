package com.ssafy.springbootbe.common.redis;

import java.util.concurrent.TimeUnit;

public interface RedisService {
    void save(String key, String value, Long ttl, TimeUnit timeUnit);
    String get(String key);
    boolean hasKey(String key);
    boolean delete(String key);
}
