package com.ssafy.springbootbe.domain.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GithubOAuthCallbackResponse {

    private String accessToken;
    private String tokenType;
    private Long userId;

    public static GithubOAuthCallbackResponse of(String accessToken, Long userId) {
        return GithubOAuthCallbackResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .userId(userId)
                .build();
    }
}
