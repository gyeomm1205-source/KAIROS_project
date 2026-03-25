package com.ssafy.springbootbe.domain.activities.service;

import com.ssafy.springbootbe.common.redis.RedisService;
import com.ssafy.springbootbe.common.utils.OAuthTokenCryptoService;
import com.ssafy.springbootbe.domain.activities.dto.response.ActivitySyncResponse;
import com.ssafy.springbootbe.domain.activities.exception.ActivitySyncFailedException;
import com.ssafy.springbootbe.persistence.activity.entity.ActivityHistory;
import com.ssafy.springbootbe.persistence.activity.repository.ActivityHistoryRepository;
import com.ssafy.springbootbe.persistence.activity.type.ActivityType;
import com.ssafy.springbootbe.persistence.oauth.entity.OAuthAccount;
import com.ssafy.springbootbe.persistence.oauth.repository.OAuthAccountRepository;
import com.ssafy.springbootbe.persistence.oauth.type.OAuthProvider;
import com.ssafy.springbootbe.persistence.user.entity.User;
import com.ssafy.springbootbe.persistence.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActivitySyncServiceImpl implements ActivitySyncService {

    private static final long EXTERNAL_ACCOUNT_CACHE_TTL_HOURS = 1L;
    private static final int RECENT_DAYS = 30;
    private static final int TITLE_MAX_LENGTH = 490;
    private static final String GITHUB_PROVIDER = "github";
    private static final String VELOG_PROVIDER = "velog";

    private final UserRepository userRepository;
    private final OAuthAccountRepository oAuthAccountRepository;
    private final ActivityHistoryRepository activityHistoryRepository;
    private final OAuthTokenCryptoService oAuthTokenCryptoService;
    private final RedisService redisService;
    private final ObjectMapper objectMapper;
    private final GithubApiService githubApiService;
    private final VelogRssService velogRssService;

    @Override
    @Transactional
    public ActivitySyncResponse syncActivities(Long userId, String provider) {
        validateProvider(provider);

        LocalDateTime syncedAt = LocalDateTime.now();
        int newCount;

        if (GITHUB_PROVIDER.equalsIgnoreCase(provider)) {
            newCount = syncGithub(userId, syncedAt);
        } else {
            newCount = syncVelog(userId, syncedAt);
        }

        log.info("활동 동기화 완료. userId={}, provider={}, newCount={}", userId, provider, newCount);
        return ActivitySyncResponse.builder()
                .provider(provider.toLowerCase())
                .syncedAt(syncedAt)
                .newCount(newCount)
                .build();
    }

    private void validateProvider(String provider) {
        if (!GITHUB_PROVIDER.equalsIgnoreCase(provider) && !VELOG_PROVIDER.equalsIgnoreCase(provider)) {
            throw new IllegalArgumentException("지원하지 않는 provider 입니다: " + provider);
        }
    }

    private int syncGithub(Long userId, LocalDateTime syncedAt) {
        OAuthAccount githubAccount = oAuthAccountRepository.findByUserUserIdAndProvider(userId, OAuthProvider.GITHUB)
                .filter(a -> a.getRefreshToken() != null)
                .orElseThrow(() -> new IllegalArgumentException("GitHub 계정이 연동되지 않았습니다."));

        String accessToken = oAuthTokenCryptoService.decrypt(githubAccount.getRefreshToken());
        GithubApiService.GithubUserDetailResponse userDetail = githubApiService.fetchUserDetail(accessToken);
        String login = userDetail.getLogin();

        Optional<LocalDateTime> lastSyncDate = activityHistoryRepository
                .findLatestActivityDateByUserIdAndActivityType(userId, ActivityType.GITHUB_COMMIT);

        List<GithubApiService.CommitInfo> newCommits = githubApiService.fetchCommitsSince(
                accessToken, login, lastSyncDate.orElse(null));

        if (!newCommits.isEmpty()) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("userId=" + userId + " 에 해당하는 유저가 없습니다."));

            List<ActivityHistory> activities = newCommits.stream()
                    .map(commit -> ActivityHistory.builder()
                            .user(user)
                            .activityType(ActivityType.GITHUB_COMMIT)
                            .title(truncate(commit.message()))
                            .activityDate(commit.createdAt())
                            .build())
                    .collect(Collectors.toList());
            activityHistoryRepository.saveAll(activities);
        }

        updateGithubCache(
                userId,
                login,
                syncedAt,
                accessToken,
                userDetail.getPublicRepos() + userDetail.getTotalPrivateRepos()
        );
        return newCommits.size();
    }

    private int syncVelog(Long userId, LocalDateTime syncedAt) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("userId=" + userId + " 에 해당하는 유저가 없습니다."));

        String velogUsername = user.getVelogUsername();
        if (velogUsername == null || velogUsername.isBlank()) {
            throw new IllegalArgumentException("Velog 계정이 연동되지 않았습니다.");
        }

        Optional<LocalDateTime> lastSyncDate = activityHistoryRepository
                .findLatestActivityDateByUserIdAndActivityType(userId, ActivityType.VELOG_POST);

        List<VelogRssService.PostInfo> newPosts = velogRssService.fetchPostsSince(
                velogUsername, lastSyncDate.orElse(null));

        if (!newPosts.isEmpty()) {
            List<ActivityHistory> activities = newPosts.stream()
                    .map(post -> ActivityHistory.builder()
                            .user(user)
                            .activityType(ActivityType.VELOG_POST)
                            .title(truncate(post.title()))
                            .activityDate(post.publishedAt())
                            .build())
                    .collect(Collectors.toList());
            activityHistoryRepository.saveAll(activities);
        }

        updateVelogCache(userId, velogUsername, syncedAt);
        return newPosts.size();
    }

    private void updateGithubCache(Long userId, String login, LocalDateTime syncedAt,
                                   String accessToken, int totalRepos) {
        long recentCommits = activityHistoryRepository.countByUserIdAndActivityTypeSince(
                userId, ActivityType.GITHUB_COMMIT, LocalDateTime.now().minusDays(RECENT_DAYS));
        int recentPrs = githubApiService.countRecentPullRequests(accessToken, login);

        Map<String, Object> cacheValue = new HashMap<>();
        cacheValue.put("username", login);
        cacheValue.put("lastSyncedAt", syncedAt.toString());
        cacheValue.put("recentCommits", recentCommits);
        cacheValue.put("recentPrs", recentPrs);
        cacheValue.put("totalRepos", totalRepos);

        saveToRedis(GITHUB_PROVIDER, "external-account:" + userId + ":github", cacheValue);
    }

    private void updateVelogCache(Long userId, String username, LocalDateTime syncedAt) {
        long totalPosts = activityHistoryRepository.countByUserIdAndActivityType(userId, ActivityType.VELOG_POST);
        Optional<LocalDateTime> latestPostDate = activityHistoryRepository
                .findLatestActivityDateByUserIdAndActivityType(userId, ActivityType.VELOG_POST);

        Map<String, Object> cacheValue = new HashMap<>();
        cacheValue.put("username", username);
        cacheValue.put("lastSyncedAt", syncedAt.toString());
        cacheValue.put("totalPosts", totalPosts);
        cacheValue.put("latestPostDate", latestPostDate.map(d -> d.toLocalDate().toString()).orElse(null));
        cacheValue.put("totalViews", 0);

        saveToRedis(VELOG_PROVIDER, "external-account:" + userId + ":velog", cacheValue);
    }

    private void saveToRedis(String provider, String key, Object value) {
        try {
            String json = objectMapper.writeValueAsString(value);
            redisService.save(key, json, EXTERNAL_ACCOUNT_CACHE_TTL_HOURS, TimeUnit.HOURS);
        } catch (RuntimeException e) {
            throw new ActivitySyncFailedException(provider + " 동기화 결과 Redis 저장에 실패했습니다.", e, true);
        }
    }

    private String truncate(String text) {
        if (text == null) return "";
        return text.length() > TITLE_MAX_LENGTH ? text.substring(0, TITLE_MAX_LENGTH) : text;
    }
}
