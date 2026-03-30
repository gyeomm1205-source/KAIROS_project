package com.ssafy.s14p21a506.testping.service;

import com.ssafy.s14p21a506.exception.BaseException;
import com.ssafy.s14p21a506.exception.ErrorCode;
import com.ssafy.s14p21a506.testping.dto.TestPingResponse;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class TestPingServiceImpl implements TestPingService {

    private final String fastApiBaseUrl;
    private final RestClient restClient;

    public TestPingServiceImpl(@Value("${app.fastapi.base-url}") String fastApiBaseUrl) {
        this.fastApiBaseUrl = fastApiBaseUrl;
        this.restClient = RestClient.create();
    }

    @Override
    public TestPingResponse pingSpring() {
        return TestPingResponse.springPong();
    }

    @Override
    public TestPingResponse pingFastApi() {
        try {
            Map<String, Object> response = restClient.get()
                    .uri(fastApiBaseUrl + "/api/test/ping")
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });

            return TestPingResponse.springToFastApi(response == null ? Map.of() : response);
        } catch (Exception e) {
            throw new BaseException(ErrorCode.FASTAPI_UNAVAILABLE, e);
        }
    }
}
