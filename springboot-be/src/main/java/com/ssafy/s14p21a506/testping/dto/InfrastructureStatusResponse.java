package com.ssafy.s14p21a506.testping.dto;

import java.time.Instant;

public record InfrastructureStatusResponse(
        String source,
        String message,
        Instant timestamp,
        InfrastructureCheckResult mysql,
        InfrastructureCheckResult redis,
        InfrastructureCheckResult fastapi,
        InfrastructureCheckResult qdrant
) {
    public static InfrastructureStatusResponse of(
            InfrastructureCheckResult mysql,
            InfrastructureCheckResult redis,
            InfrastructureCheckResult fastapi,
            InfrastructureCheckResult qdrant
    ) {
        return new InfrastructureStatusResponse(
                "springboot-be",
                "infrastructure-check-completed",
                Instant.now(),
                mysql,
                redis,
                fastapi,
                qdrant
        );
    }
}
