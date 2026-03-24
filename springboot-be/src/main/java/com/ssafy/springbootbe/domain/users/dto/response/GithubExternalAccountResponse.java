package com.ssafy.springbootbe.domain.users.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GithubExternalAccountResponse {

    private String username;
    private LocalDateTime lastSyncedAt;
    private int recentCommits;
    private int recentPrs;
    private int totalRepos;
}
