package com.ssafy.s14p21a506.testping.service;

import com.ssafy.s14p21a506.exception.BaseException;
import com.ssafy.s14p21a506.exception.ErrorCode;
import com.ssafy.s14p21a506.testping.dto.InfrastructureCheckResult;
import com.ssafy.s14p21a506.testping.dto.InfrastructureStatusResponse;
import com.ssafy.s14p21a506.testping.dto.TestPingResponse;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.RestClient;

@Service
public class TestPingServiceImpl implements TestPingService {

    private final String fastApiBaseUrl;
    private final JdbcTemplate jdbcTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    private final RestClient restClient;

    public TestPingServiceImpl(
            @Value("${app.fastapi.base-url}") String fastApiBaseUrl,
            JdbcTemplate jdbcTemplate,
            StringRedisTemplate stringRedisTemplate
    ) {
        this.fastApiBaseUrl = fastApiBaseUrl;
        this.jdbcTemplate = jdbcTemplate;
        this.stringRedisTemplate = stringRedisTemplate;
        this.restClient = RestClient.create();
    }

    @Override
    public TestPingResponse pingSpring() {
        return TestPingResponse.springPong();
    }

    @Override
    public TestPingResponse pingFastApi() {
        try {
            Map<String, Object> response = callFastApi("/api/test/ping");

            return TestPingResponse.springToFastApi(response == null ? Map.of() : response);
        } catch (Exception e) {
            throw new BaseException(ErrorCode.FASTAPI_UNAVAILABLE, e);
        }
    }

    @Override
    public InfrastructureStatusResponse checkInfrastructure() {
        InfrastructureCheckResult mysql = runCheck("mysql", this::inspectMysql);
        InfrastructureCheckResult redis = runCheck("redis", this::inspectRedis);
        InfrastructureCheckResult fastApi = runCheck("fastapi", () -> inspectFastApi("/api/test/ping"));
        InfrastructureCheckResult qdrant = runCheck("qdrant", () -> inspectFastApi("/api/test/qdrant"));

        return InfrastructureStatusResponse.of(mysql, redis, fastApi, qdrant);
    }

    private Map<String, Object> inspectMysql() {
        LinkedHashMap<String, Object> details = new LinkedHashMap<>();
        details.put("result", jdbcTemplate.queryForObject("select 1", Integer.class));
        details.put("database", jdbcTemplate.queryForObject("select database()", String.class));
        return details;
    }

    private Map<String, Object> inspectRedis() {
        LinkedHashMap<String, Object> details = new LinkedHashMap<>();
        details.put("ping", stringRedisTemplate.execute((RedisCallback<String>) connection -> connection.ping()));
        details.put("keyspace", "db0");
        return details;
    }

    private Map<String, Object> inspectFastApi(String path) {
        LinkedHashMap<String, Object> details = new LinkedHashMap<>();
        details.put("baseUrl", fastApiBaseUrl);
        details.put("response", callFastApi(path));
        return details;
    }

    private InfrastructureCheckResult runCheck(String name, Supplier<Object> action) {
        try {
            return InfrastructureCheckResult.up(name, action.get());
        } catch (Exception e) {
            LinkedHashMap<String, Object> details = new LinkedHashMap<>();
            details.put("message", extractRootMessage(e));
            return InfrastructureCheckResult.down(name, details);
        }
    }

    private Map<String, Object> callFastApi(String path) {
        return restClient.get()
                .uri(fastApiBaseUrl + path)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
    }

    private String extractRootMessage(Exception e) {
        Throwable current = e;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }

        return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
    }
}
