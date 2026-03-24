package com.ssafy.springbootbe.domain.users.service;

import com.ssafy.springbootbe.common.dto.DevPositionInfo;
import com.ssafy.springbootbe.common.dto.TechStackInfo;
import com.ssafy.springbootbe.common.jwt.JWTUtils;
import com.ssafy.springbootbe.common.redis.RedisService;
import com.ssafy.springbootbe.common.utils.OAuthTokenCryptoService;
import com.ssafy.springbootbe.domain.users.dto.request.DarkModeUpdateRequest;
import com.ssafy.springbootbe.domain.users.dto.request.UserProfileUpdateRequest;
import com.ssafy.springbootbe.domain.users.dto.response.ExternalAccountsResponse;
import com.ssafy.springbootbe.domain.users.dto.response.GithubExternalAccountResponse;
import com.ssafy.springbootbe.domain.users.dto.response.UserProfileResponse;
import com.ssafy.springbootbe.domain.users.dto.response.UserProfileUpdateResponse;
import com.ssafy.springbootbe.domain.users.dto.response.VelogExternalAccountResponse;
import com.ssafy.springbootbe.domain.users.exception.ExternalAccountCacheException;
import com.ssafy.springbootbe.domain.users.exception.ExternalAccountFetchFailedException;
import com.ssafy.springbootbe.domain.users.exception.GithubExternalAccountNotFoundException;
import com.ssafy.springbootbe.domain.users.exception.UserNotFoundException;
import com.ssafy.springbootbe.domain.users.exception.VelogUsernameNotFoundException;
import com.ssafy.springbootbe.persistence.activity.entity.ActivityHistory;
import com.ssafy.springbootbe.persistence.activity.repository.ActivityHistoryRepository;
import com.ssafy.springbootbe.persistence.activity.type.ActivityType;
import com.ssafy.springbootbe.persistence.oauth.entity.OAuthAccount;
import com.ssafy.springbootbe.persistence.oauth.repository.OAuthAccountRepository;
import com.ssafy.springbootbe.persistence.oauth.type.OAuthProvider;
import com.ssafy.springbootbe.persistence.position.entity.DevPosition;
import com.ssafy.springbootbe.persistence.position.repository.DevPositionRepository;
import com.ssafy.springbootbe.persistence.techstack.entity.TechStack;
import com.ssafy.springbootbe.persistence.techstack.repository.TechStackRepository;
import com.ssafy.springbootbe.persistence.user.entity.User;
import com.ssafy.springbootbe.persistence.user.entity.UserCurriculumCategory;
import com.ssafy.springbootbe.persistence.user.entity.UserDesiredPosition;
import com.ssafy.springbootbe.persistence.user.entity.UserTechStack;
import com.ssafy.springbootbe.persistence.user.repository.UserCurriculumCategoryRepository;
import com.ssafy.springbootbe.persistence.user.repository.UserDesiredPositionRepository;
import com.ssafy.springbootbe.persistence.user.repository.UserRepository;
import com.ssafy.springbootbe.persistence.user.repository.UserTechStackRepository;
import com.ssafy.springbootbe.persistence.user.type.CurriculumCategory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class UsersServiceImpl implements UsersService {

    private static final long PROFILE_CACHE_TTL_HOURS = 128L;
    private static final long EXTERNAL_ACCOUNT_CACHE_TTL_HOURS = 1L;
    private static final int RECENT_GITHUB_DAYS = 30;
    private static final String GITHUB_CACHE_KEY_PREFIX = "external-account:";
    private static final String GITHUB_CACHE_PROVIDER = "github";
    private static final String VELOG_CACHE_PROVIDER = "velog";

    private final UserRepository userRepository;
    private final UserDesiredPositionRepository userDesiredPositionRepository;
    private final UserTechStackRepository userTechStackRepository;
    private final UserCurriculumCategoryRepository userCurriculumCategoryRepository;
    private final DevPositionRepository devPositionRepository;
    private final TechStackRepository techStackRepository;
    private final OAuthAccountRepository oAuthAccountRepository;
    private final ActivityHistoryRepository activityHistoryRepository;
    private final RedisService redisService;
    private final ObjectMapper objectMapper;
    private final JWTUtils jwtUtils;
    private final OAuthTokenCryptoService oAuthTokenCryptoService;

    @Value("${oauth.github.accept-vnd}")
    private String githubAcceptVnd;

    @Value("${oauth.github.api-version}")
    private String githubApiVersion;

    @Value("${oauth.github.user-info-url}")
    private String githubUserInfoUrl;

    @Value("${oauth.github.user-agent}")
    private String githubUserAgent;

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse findProfile(Long userId) {
        String cacheKey = buildProfileCacheKey(userId);
        String cachedValue = redisService.get(cacheKey);

        if (cachedValue != null) {
            UserProfileResponse cached = deserializeProfile(cachedValue);
            if (cached != null) {
                return cached;
            }
        }

        UserProfileResponse response = buildProfileResponse(userId);
        cacheProfile(cacheKey, response);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public ExternalAccountsResponse findExternalAccounts(Long userId) {
        GithubExternalAccountResponse github = findGithubExternalAccount(userId);
        VelogExternalAccountResponse velog = findVelogExternalAccount(userId);

        return ExternalAccountsResponse.builder()
                .github(github)
                .velog(velog)
                .build();
    }

    @Override
    @Transactional
    public UserProfileUpdateResponse updateProfile(Long userId, UserProfileUpdateRequest request) {
        User user = findUserByIdOrThrow(userId);
        user.updateProfile(request.getPosition(), request.getConsiderPersonalSchedule());

        if (request.getDesiredPositionIds() != null) {
            userDesiredPositionRepository.deleteByUserUserId(userId);
            List<DevPosition> devPositions = devPositionRepository.findAllById(request.getDesiredPositionIds());
            List<UserDesiredPosition> newDesiredPositions = devPositions.stream()
                    .map(dp -> UserDesiredPosition.builder().user(user).devPosition(dp).build())
                    .toList();
            userDesiredPositionRepository.saveAll(newDesiredPositions);
        }

        if (request.getTechStackIds() != null) {
            List<UserTechStack> existing = userTechStackRepository.findByUserUserId(userId);
            Map<Long, Integer> scoreMap = new HashMap<>();
            for (UserTechStack uts : existing) {
                scoreMap.put(uts.getTechStack().getTechStackId(), uts.getScore());
            }
            userTechStackRepository.deleteByUserUserId(userId);
            List<TechStack> techStacks = techStackRepository.findAllById(request.getTechStackIds());
            List<UserTechStack> newTechStacks = techStacks.stream()
                    .map(ts -> UserTechStack.builder()
                            .user(user)
                            .techStack(ts)
                            .score(scoreMap.getOrDefault(ts.getTechStackId(), 0))
                            .build())
                    .toList();
            userTechStackRepository.saveAll(newTechStacks);
        }

        if (request.getCurriculumCategories() != null) {
            userCurriculumCategoryRepository.deleteByUserUserId(userId);
            List<UserCurriculumCategory> newCategories = request.getCurriculumCategories().stream()
                    .map(cat -> UserCurriculumCategory.builder().user(user).category(cat).build())
                    .toList();
            userCurriculumCategoryRepository.saveAll(newCategories);
        }

        redisService.delete(buildProfileCacheKey(userId));

        List<DevPositionInfo> desiredPositions = userDesiredPositionRepository.findByUserUserId(userId).stream()
                .map(udp -> DevPositionInfo.from(udp.getDevPosition()))
                .toList();
        List<TechStackInfo> techStacks = userTechStackRepository.findByUserUserId(userId).stream()
                .map(uts -> TechStackInfo.from(uts.getTechStack()))
                .toList();
        List<CurriculumCategory> curriculumCategories = userCurriculumCategoryRepository.findByUserUserId(userId).stream()
                .map(UserCurriculumCategory::getCategory)
                .toList();

        log.info("유저 프로필 수정 완료. userId={}", userId);

        return UserProfileUpdateResponse.builder()
                .position(user.getPosition())
                .desiredPositions(desiredPositions)
                .techStacks(techStacks)
                .curriculumCategories(curriculumCategories)
                .considerPersonalSchedule(user.getConsiderPersonalSchedule())
                .build();
    }

    @Override
    @Transactional
    public Map<String, Boolean> updateDarkMode(Long userId, DarkModeUpdateRequest request) {
        User user = findUserByIdOrThrow(userId);
        user.updateDarkMode(request.getDarkModeEnabled());
        redisService.delete(buildProfileCacheKey(userId));
        log.info("다크모드 설정 변경. userId={}, darkModeEnabled={}", userId, request.getDarkModeEnabled());
        return Map.of("darkModeEnabled", user.getDarkModeEnabled());
    }


    @Override
    @Transactional
    public void deleteUser(Long userId, String token) {
        User user = findUserByIdOrThrow(userId);
        redisService.delete(buildProfileCacheKey(userId));
        userRepository.delete(user);
        blacklistToken(token);
        log.info("회원 탈퇴 완료. userId={}", userId);
    }

    private void blacklistToken(String token) {
        Date expiration = jwtUtils.getClaims(token).getExpiration();
        long remainingMillis = expiration.getTime() - System.currentTimeMillis();
        if (remainingMillis > 0) {
            redisService.save("blacklist:" + token, "deleted", remainingMillis, TimeUnit.MILLISECONDS);
        }
    }

    protected User findUserByIdOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    GithubExternalAccountResponse findGithubExternalAccount(Long userId) {
        String cacheKey = buildExternalAccountCacheKey(userId, GITHUB_CACHE_PROVIDER);
        GithubExternalAccountResponse cached = readCachedExternalAccount(cacheKey, GithubExternalAccountResponse.class);
        if (cached != null) {
            return cached;
        }

        GithubExternalAccountResponse response = loadGithubExternalAccount(userId);
        cacheExternalAccount(cacheKey, response);
        return response;
    }

    VelogExternalAccountResponse findVelogExternalAccount(Long userId) {
        String cacheKey = buildExternalAccountCacheKey(userId, VELOG_CACHE_PROVIDER);
        VelogExternalAccountResponse cached = readCachedExternalAccount(cacheKey, VelogExternalAccountResponse.class);
        if (cached != null) {
            return cached;
        }

        VelogExternalAccountResponse response = loadVelogExternalAccount(userId);
        cacheExternalAccount(cacheKey, response);
        return response;
    }

    GithubExternalAccountResponse loadGithubExternalAccount(Long userId) {
        OAuthAccount githubAccount = oAuthAccountRepository.findByUserUserIdAndProvider(userId, OAuthProvider.GITHUB)
                .orElseThrow(() -> new GithubExternalAccountNotFoundException(userId));

        String encryptedGithubToken = githubAccount.getRefreshToken();
        if (encryptedGithubToken == null || encryptedGithubToken.isBlank()) {
            throw new GithubExternalAccountNotFoundException(userId);
        }

        String githubToken;
        try {
            githubToken = oAuthTokenCryptoService.decrypt(encryptedGithubToken);
        } catch (RuntimeException e) {
            throw new ExternalAccountFetchFailedException("GitHub access token 복호화에 실패했습니다.", e);
        }

        try {
            JsonNode userInfo = requestGithubJson(
                    githubUserInfoUrl,
                    githubToken,
                    MediaType.APPLICATION_JSON_VALUE
            );
            String username = readText(userInfo, "login");
            if (username == null || username.isBlank()) {
                throw new ExternalAccountFetchFailedException("GitHub username 조회에 실패했습니다.");
            }

            int totalRepos = readInt(userInfo, "public_repos");

            return GithubExternalAccountResponse.builder()
                    .username(username)
                    .lastSyncedAt(findLatestSyncedAt(userId, ActivityType.GITHUB_COMMIT))
                    .recentCommits(fetchGithubRecentCommitCount(githubToken, username))
                    .recentPrs(fetchGithubRecentPrCount(githubToken, username))
                    .totalRepos(totalRepos)
                    .build();
        } catch (ExternalAccountFetchFailedException e) {
            throw e;
        } catch (RestClientException e) {
            throw new ExternalAccountFetchFailedException("GitHub 외부 API 호출에 실패했습니다.", e);
        }
    }

    VelogExternalAccountResponse loadVelogExternalAccount(Long userId) {
        User user = findUserByIdOrThrow(userId);
        String velogUsername = user.getVelogUsername();
        if (velogUsername == null || velogUsername.isBlank()) {
            throw new VelogUsernameNotFoundException(userId);
        }

        long totalPosts = activityHistoryRepository.countByUserIdAndActivityType(userId, ActivityType.VELOG_POST);
        LocalDate latestPostDate = activityHistoryRepository
                .findLatestActivityDateByUserIdAndActivityType(userId, ActivityType.VELOG_POST)
                .map(LocalDateTime::toLocalDate)
                .orElse(null);

        return VelogExternalAccountResponse.builder()
                .username(velogUsername)
                .lastSyncedAt(findLatestSyncedAt(userId, ActivityType.VELOG_POST))
                .totalPosts((int) totalPosts)
                .latestPostDate(latestPostDate)
                .totalViews(0L)
                .build();
    }

    private UserProfileResponse buildProfileResponse(Long userId) {
        User user = findUserByIdOrThrow(userId);
        List<DevPositionInfo> desiredPositions = userDesiredPositionRepository.findByUserUserId(userId).stream()
                .map(udp -> DevPositionInfo.from(udp.getDevPosition()))
                .toList();
        List<TechStackInfo> techStacks = userTechStackRepository.findByUserUserId(userId).stream()
                .map(uts -> TechStackInfo.from(uts.getTechStack()))
                .toList();
        List<CurriculumCategory> curriculumCategories = userCurriculumCategoryRepository.findByUserUserId(userId).stream()
                .map(UserCurriculumCategory::getCategory)
                .toList();
        return UserProfileResponse.of(user, desiredPositions, techStacks, curriculumCategories);
    }

    private void cacheProfile(String cacheKey, UserProfileResponse response) {
        try {
            String json = objectMapper.writeValueAsString(response);
            redisService.save(cacheKey, json, PROFILE_CACHE_TTL_HOURS, TimeUnit.HOURS);
        } catch (JacksonException e) {
            log.warn("유저 프로필 캐시 직렬화 실패. key={}", cacheKey, e);
        }
    }

    private UserProfileResponse deserializeProfile(String json) {
        try {
            return objectMapper.readValue(json, UserProfileResponse.class);
        } catch (JacksonException e) {
            log.warn("유저 프로필 캐시 역직렬화 실패", e);
            return null;
        }
    }

    private String buildProfileCacheKey(Long userId) {
        return "user:profile:" + userId;
    }

    private <T> T readCachedExternalAccount(String cacheKey, Class<T> type) {
        final String cachedValue;
        try {
            cachedValue = redisService.get(cacheKey);
        } catch (RuntimeException e) {
            throw new ExternalAccountCacheException("Redis 외부 계정 캐시 조회에 실패했습니다. key=" + cacheKey, e);
        }

        if (cachedValue == null || cachedValue.isBlank()) {
            return null;
        }

        try {
            return objectMapper.readValue(cachedValue, type);
        } catch (JacksonException e) {
            log.warn("외부 계정 캐시 역직렬화 실패. key={}", cacheKey, e);
            return null;
        }
    }

    private void cacheExternalAccount(String cacheKey, Object response) {
        try {
            redisService.save(
                    cacheKey,
                    objectMapper.writeValueAsString(response),
                    EXTERNAL_ACCOUNT_CACHE_TTL_HOURS,
                    TimeUnit.HOURS
            );
        } catch (JacksonException e) {
            throw new ExternalAccountCacheException("외부 계정 캐시 직렬화에 실패했습니다. key=" + cacheKey, e);
        } catch (RuntimeException e) {
            throw new ExternalAccountCacheException("Redis 외부 계정 캐시 저장에 실패했습니다. key=" + cacheKey, e);
        }
    }

    private String buildExternalAccountCacheKey(Long userId, String provider) {
        return GITHUB_CACHE_KEY_PREFIX + userId + ":" + provider;
    }

    private LocalDateTime findLatestSyncedAt(Long userId, ActivityType activityType) {
        return activityHistoryRepository
                .findTopByUserUserIdAndActivityTypeOrderByActivityDateDesc(userId, activityType)
                .map(ActivityHistory::getActivityDate)
                .orElse(null);
    }

    private JsonNode requestGithubJson(String uri, String githubToken, String acceptHeader) {
        String response = RestClient.create()
                .get()
                .uri(uri)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + githubToken)
                .header(HttpHeaders.ACCEPT, acceptHeader)
                .header("X-GitHub-Api-Version", githubApiVersion)
                .header(HttpHeaders.USER_AGENT, githubUserAgent)
                .retrieve()
                .body(String.class);

        if (response == null || response.isBlank()) {
            throw new ExternalAccountFetchFailedException("GitHub API 응답이 비어 있습니다.");
        }

        try {
            return objectMapper.readTree(response);
        } catch (JacksonException e) {
            throw new ExternalAccountFetchFailedException("GitHub API 응답 파싱에 실패했습니다.", e);
        }
    }

    private int fetchGithubRecentCommitCount(String githubToken, String username) {
        LocalDate sinceDate = LocalDate.now().minusDays(RECENT_GITHUB_DAYS);
        String query = "author:" + username + " author-date:>=" + sinceDate;
        return fetchGithubSearchTotalCount(
                githubToken,
                buildGithubApiBaseUrl() + "/search/commits?q=" + encode(query),
                "application/vnd.github+json"
        );
    }

    private int fetchGithubRecentPrCount(String githubToken, String username) {
        LocalDate sinceDate = LocalDate.now().minusDays(RECENT_GITHUB_DAYS);
        String query = "author:" + username + " type:pr created:>=" + sinceDate;
        return fetchGithubSearchTotalCount(
                githubToken,
                buildGithubApiBaseUrl() + "/search/issues?q=" + encode(query),
                MediaType.APPLICATION_JSON_VALUE
        );
    }

    private int fetchGithubSearchTotalCount(String githubToken, String uri, String acceptHeader) {
        JsonNode payload = requestGithubJson(uri, githubToken, acceptHeader);
        JsonNode totalCount = payload.get("total_count");
        if (totalCount == null || !totalCount.isNumber()) {
            throw new ExternalAccountFetchFailedException("GitHub 검색 응답에 total_count가 없습니다.");
        }
        return totalCount.asInt();
    }

    private String buildGithubApiBaseUrl() {
        int userPathIndex = githubUserInfoUrl.lastIndexOf("/user");
        if (userPathIndex <= 0) {
            return "https://api.github.com";
        }
        return githubUserInfoUrl.substring(0, userPathIndex);
    }

    private String readText(JsonNode node, String fieldName) {
        JsonNode value = node.get(fieldName);
        return value == null || value.isNull() ? null : value.asText();
    }

    private int readInt(JsonNode node, String fieldName) {
        JsonNode value = node.get(fieldName);
        return value == null || !value.isNumber() ? 0 : value.asInt();
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
