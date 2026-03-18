package com.ssafy.springbootbe.domain.auth.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GithubCollectAsyncRequest {

    private Long userId;
    private String githubToken;
    private String githubUsername;
}
