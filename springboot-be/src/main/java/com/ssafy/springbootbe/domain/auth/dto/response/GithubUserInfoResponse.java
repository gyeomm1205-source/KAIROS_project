package com.ssafy.springbootbe.domain.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GithubUserInfoResponse {

    private Long id;
    private String login;
    private String email;

    @JsonProperty("avatar_url")
    private String avatarUrl;
}
