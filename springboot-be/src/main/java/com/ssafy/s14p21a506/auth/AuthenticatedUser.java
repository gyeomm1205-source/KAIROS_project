package com.ssafy.s14p21a506.auth;

import java.util.Collection;
import java.util.List;
import org.springframework.security.oauth2.jwt.Jwt;

public record AuthenticatedUser(
        String subject,
        String username,
        String email,
        List<String> groups,
        String tokenUse,
        String clientId
) {

    public static AuthenticatedUser from(Jwt jwt, String usernameClaim) {
        return new AuthenticatedUser(
                jwt.getSubject(),
                resolveUsername(jwt, usernameClaim),
                jwt.getClaimAsString("email"),
                resolveGroups(jwt),
                jwt.getClaimAsString("token_use"),
                resolveClientId(jwt)
        );
    }

    private static String resolveUsername(Jwt jwt, String usernameClaim) {
        String configured = jwt.getClaimAsString(usernameClaim);
        if (configured != null && !configured.isBlank()) {
            return configured;
        }

        String fallback = jwt.getClaimAsString("cognito:username");
        if (fallback != null && !fallback.isBlank()) {
            return fallback;
        }

        fallback = jwt.getClaimAsString("email");
        if (fallback != null && !fallback.isBlank()) {
            return fallback;
        }

        return jwt.getSubject();
    }

    private static List<String> resolveGroups(Jwt jwt) {
        Object groupsClaim = jwt.getClaims().get("cognito:groups");
        if (groupsClaim instanceof Collection<?> collection) {
            return collection.stream()
                    .filter(value -> value != null && !value.toString().isBlank())
                    .map(Object::toString)
                    .toList();
        }
        return List.of();
    }

    private static String resolveClientId(Jwt jwt) {
        String clientId = jwt.getClaimAsString("client_id");
        if (clientId != null && !clientId.isBlank()) {
            return clientId;
        }

        List<String> audience = jwt.getAudience();
        if (!audience.isEmpty()) {
            return audience.getFirst();
        }

        return null;
    }
}
