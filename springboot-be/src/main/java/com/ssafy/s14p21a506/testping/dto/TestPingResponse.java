package com.ssafy.s14p21a506.testping.dto;

import java.time.Instant;

public record TestPingResponse(
        String source,
        String message,
        Instant timestamp,
        Object downstream
) {
    public static TestPingResponse springPong() {
        return new TestPingResponse("springboot-be", "pong", Instant.now(), null);
    }

    public static TestPingResponse springToFastApi(Object fastApiResponse) {
        return new TestPingResponse("springboot-be", "spring-to-fastapi-ok", Instant.now(), fastApiResponse);
    }
}
