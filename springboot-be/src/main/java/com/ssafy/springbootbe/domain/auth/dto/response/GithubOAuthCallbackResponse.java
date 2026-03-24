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
    private String githubTaskId;

    public static GithubOAuthCallbackResponse of(String accessToken, Long userId, String githubTaskId) {
        return GithubOAuthCallbackResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .userId(userId)
                .githubTaskId(githubTaskId)
                .build();
    }
}
