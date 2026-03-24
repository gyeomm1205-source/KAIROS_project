package com.ssafy.springbootbe.domain.activities.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ssafy.springbootbe.domain.activities.exception.ActivitySyncFailedException;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GithubApiService {

    private static final String GITHUB_API_BASE = "https://api.github.com";
    private static final String EVENTS_URL_TEMPLATE = GITHUB_API_BASE + "/users/%s/events?per_page=100";
    private static final DateTimeFormatter GITHUB_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

    @Value("${oauth.github.accept-vnd}")
    private String acceptVnd;

    @Value("${oauth.github.api-version}")
    private String apiVersion;

    @Value("${oauth.github.user-agent}")
    private String userAgent;

    @Value("${oauth.github.user-info-url}")
    private String userInfoUrl;

    public GithubUserDetailResponse fetchUserDetail(String accessToken) {
        try {
            GithubUserDetailResponse response = RestClient.create()
                    .get()
                    .uri(userInfoUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .header(HttpHeaders.ACCEPT, acceptVnd)
                    .header("X-GitHub-Api-Version", apiVersion)
                    .header(HttpHeaders.USER_AGENT, userAgent)
                    .retrieve()
                    .body(GithubUserDetailResponse.class);

            if (response == null || response.getLogin() == null) {
                throw new ActivitySyncFailedException("GitHub", new IllegalStateException("GitHub 사용자 정보가 없습니다."));
            }
            return response;
        } catch (RestClientException e) {
            throw new ActivitySyncFailedException("GitHub", e);
        }
    }

    public List<CommitInfo> fetchCommitsSince(String accessToken, String login, LocalDateTime since) {
        try {
            List<GithubEventResponse> events = RestClient.create()
                    .get()
                    .uri(String.format(EVENTS_URL_TEMPLATE, login))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .header(HttpHeaders.ACCEPT, acceptVnd)
                    .header("X-GitHub-Api-Version", apiVersion)
                    .header(HttpHeaders.USER_AGENT, userAgent)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<GithubEventResponse>>() {});

            if (events == null) return List.of();

            List<CommitInfo> commits = new ArrayList<>();
            for (GithubEventResponse event : events) {
                if (!"PushEvent".equals(event.getType())) continue;

                LocalDateTime eventTime = parseGithubDate(event.getCreatedAt());
                if (since != null && !eventTime.isAfter(since)) continue;

                if (event.getPayload() == null || event.getPayload().getCommits() == null) continue;
                for (GithubEventResponse.GithubPayload.GithubCommit commit : event.getPayload().getCommits()) {
                    if (commit.getMessage() != null && !commit.getMessage().isBlank()) {
                        commits.add(new CommitInfo(commit.getMessage(), eventTime));
                    }
                }
            }
            return commits;
        } catch (RestClientException e) {
            throw new ActivitySyncFailedException("GitHub", e);
        }
    }

    public int countRecentPullRequests(String accessToken, String login) {
        try {
            List<GithubEventResponse> events = RestClient.create()
                    .get()
                    .uri(String.format(EVENTS_URL_TEMPLATE, login))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .header(HttpHeaders.ACCEPT, acceptVnd)
                    .header("X-GitHub-Api-Version", apiVersion)
                    .header(HttpHeaders.USER_AGENT, userAgent)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<GithubEventResponse>>() {});

            if (events == null) return 0;

            LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
            return (int) events.stream()
                    .filter(e -> "PullRequestEvent".equals(e.getType()))
                    .filter(e -> e.getCreatedAt() != null && parseGithubDate(e.getCreatedAt()).isAfter(thirtyDaysAgo))
                    .count();
        } catch (RestClientException e) {
            log.warn("GitHub PR 이벤트 조회 실패. login={}", login, e);
            return 0;
        }
    }

    private LocalDateTime parseGithubDate(String dateStr) {
        if (dateStr == null) return LocalDateTime.MIN;
        try {
            return LocalDateTime.parse(dateStr, GITHUB_DATE_FORMAT);
        } catch (Exception e) {
            return LocalDateTime.MIN;
        }
    }

    // === Inner DTOs ===

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GithubUserDetailResponse {
        private String login;
        @JsonProperty("public_repos")
        private int publicRepos;
    }

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GithubEventResponse {
        private String type;
        private GithubPayload payload;
        @JsonProperty("created_at")
        private String createdAt;

        @Getter
        @NoArgsConstructor
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class GithubPayload {
            private List<GithubCommit> commits;

            @Getter
            @NoArgsConstructor
            @JsonIgnoreProperties(ignoreUnknown = true)
            public static class GithubCommit {
                private String message;
            }
        }
    }

    public record CommitInfo(String message, LocalDateTime createdAt) {}
}
