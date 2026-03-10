package com.ssafy.s14p21a506.testping.dto;

public record InfrastructureCheckResult(
        String name,
        String status,
        Object details
) {
    public static InfrastructureCheckResult up(String name, Object details) {
        return new InfrastructureCheckResult(name, "UP", details);
    }

    public static InfrastructureCheckResult down(String name, Object details) {
        return new InfrastructureCheckResult(name, "DOWN", details);
    }
}
