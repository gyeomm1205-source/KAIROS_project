package com.ssafy.springbootbe.domain.auth.service;

import com.ssafy.springbootbe.common.jwt.JWTUtils;
import com.ssafy.springbootbe.common.redis.RedisService;
import com.ssafy.springbootbe.common.utils.AIRestClient;
import com.ssafy.springbootbe.common.utils.OAuthTokenCryptoService;
import com.ssafy.springbootbe.domain.auth.dto.request.GithubCollectAsyncRequest;
import com.ssafy.springbootbe.domain.auth.dto.response.AuthTokenBundle;
import com.ssafy.springbootbe.domain.auth.dto.response.GithubAuthTokenBundle;
import com.ssafy.springbootbe.domain.auth.dto.response.GithubCollectAsyncResponse;
import com.ssafy.springbootbe.domain.auth.dto.response.GithubOAuthCallbackResponse;
import com.ssafy.springbootbe.domain.auth.dto.response.GithubTokenResponse;
import com.ssafy.springbootbe.domain.auth.dto.response.GithubUserInfoResponse;
import com.ssafy.springbootbe.domain.auth.dto.response.GoogleOAuthCallbackResponse;
import com.ssafy.springbootbe.domain.auth.dto.response.GoogleTokenResponse;
import com.ssafy.springbootbe.domain.auth.dto.response.GoogleUserInfoResponse;
import com.ssafy.springbootbe.domain.auth.exception.AuthPersistenceException;
import com.ssafy.springbootbe.domain.auth.exception.AuthRedisSaveFailedException;
import com.ssafy.springbootbe.domain.auth.exception.DuplicateOAuthEmailException;
import com.ssafy.springbootbe.domain.auth.exception.DuplicateGithubAccountException;
import com.ssafy.springbootbe.domain.auth.exception.GithubAuthorizationCodeMissingException;
import com.ssafy.springbootbe.domain.auth.exception.GithubRedirectGenerationException;
import com.ssafy.springbootbe.domain.auth.exception.GithubTokenExchangeFailedException;
import com.ssafy.springbootbe.domain.auth.exception.GithubUserInfoFetchFailedException;
import com.ssafy.springbootbe.domain.auth.exception.GoogleAuthorizationCodeMissingException;
import com.ssafy.springbootbe.domain.auth.exception.GoogleTokenExchangeFailedException;
import com.ssafy.springbootbe.domain.auth.exception.GoogleUserInfoFetchFailedException;
import com.ssafy.springbootbe.domain.auth.exception.InvalidOnboardingTokenException;
import com.ssafy.springbootbe.persistence.oauth.entity.OAuthAccount;
import com.ssafy.springbootbe.persistence.oauth.repository.OAuthAccountRepository;
import com.ssafy.springbootbe.persistence.oauth.type.OAuthProvider;
import com.ssafy.springbootbe.persistence.user.entity.User;
import com.ssafy.springbootbe.persistence.user.repository.UserRepository;
import com.ssafy.springbootbe.persistence.user.type.UserStatus;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.URI;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final long ONBOARDING_TOKEN_TTL_SECONDS = 1800L;
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String ONBOARDING_TOKEN_SUBJECT = "onboarding";
    private static final String ONBOARDING_TOKEN_PURPOSE = "onboarding";
    private static final String ONBOARDING_REDIS_KEY_PREFIX = "onboarding:";

    private final OAuthAccountRepository oAuthAccountRepository;
    private final UserRepository userRepository;
    private final RedisService redisService;
    private final JWTUtils jwtUtils;
    private final OAuthTokenCryptoService oAuthTokenCryptoService;

    @Value("${oauth.google.client_id}")
    private String googleClientId;

    @Value("${oauth.google.client_secret}")
    private String googleClientSecret;

    @Value("${oauth.google.redirect_uri}")
    private String googleRedirectUri;

    @Value("${oauth.google.token-url}")
    private String googleTokenUrl;

    @Value("${oauth.google.user-info-url}")
    private String googleUserInfoUrl;

    @Value("${oauth.github.auth_uri}")
    private String githubAuthUri;

    @Value("${oauth.github.client_id}")
    private String githubClientId;

    @Value("${oauth.github.redirect_uri}")
    private String githubRedirectUri;

    @Value("${oauth.github.scope}")
    private String githubScope;

    @Value("${oauth.github.client_secret}")
    private String githubClientSecret;

    @Value("${oauth.github.token-url}")
    private String githubTokenUrl;

    @Value("${oauth.github.accept-vnd}")
    private String githubAcceptVnd;

    @Value("${oauth.github.api-version}")
    private String githubApiVersion;

    @Value("${oauth.content-type}")
    private String oauthContentType;

    @Value("${oauth.grant_type}")
    private String oauthGrantType;

    @Value("${service.refresh-token-duration}")
    private Long refreshTokenDurationTime;

    @Value("${ai.server-url}")
    private String aiServerUrl;

    @Value("${ai.collect-async-path}")
    private String aiCollectAsyncPath;

    @Value("${oauth.github.user-info-url}")
    private String githubUserInfoUrl;

    @Value("${oauth.github.user-agent}")
    private String githubUserAgent;

    @Override
    @Transactional
    public AuthTokenBundle handleGoogleCallback(String code) {
        validateAuthorizationCode(code);

        GoogleTokenResponse tokenResponse = exchangeGoogleToken(code);
        GoogleUserInfoResponse userInfoResponse = fetchGoogleUserInfo(tokenResponse.getAccessToken());
        validateGoogleUserInfo(userInfoResponse);

        Optional<OAuthAccount> existingGoogleAccount = oAuthAccountRepository.findByProviderAndProviderAccountId(
                OAuthProvider.GOOGLE,
                userInfoResponse.getSub()
        );

        if (existingGoogleAccount.isPresent()) {
            return handleExistingUser(existingGoogleAccount.get(), tokenResponse);
        }

        if (userRepository.findByEmail(userInfoResponse.getEmail()).isPresent()) {
            throw new DuplicateOAuthEmailException(userInfoResponse.getEmail());
        }

        return handleNewUser(userInfoResponse);
    }

    @Override
    public URI buildGithubAuthorizationRedirect(String authorizationHeader) {
        Claims claims = validateOnboardingToken(authorizationHeader);
        String onboardingToken = extractBearerToken(authorizationHeader);
        String googleSub = extractGoogleSub(claims);
        validateOnboardingRedisState(googleSub);

        try {
            return UriComponentsBuilder.fromUriString(githubAuthUri)
                    .queryParam("client_id", githubClientId)
                    .queryParam("redirect_uri", githubRedirectUri)
                    .queryParam("response_type", "code")
                    .queryParam("scope", githubScope)
                    .queryParam("state", onboardingToken)
                    .build()
                    .encode()
                    .toUri();
        } catch (RuntimeException e) {
            throw new GithubRedirectGenerationException("GitHub 인증 페이지 URL 생성에 실패했습니다.", e);
        }
    }

    @Override
    @Transactional
    public GithubAuthTokenBundle handleGithubCallback(String code, String state) {
        validateGithubAuthorizationCode(code);
        Claims claims = validateOnboardingTokenWithState(state);
        String googleSub = extractGoogleSub(claims);
        OnboardingData onboardingData = findOnboardingData(googleSub);

        GithubTokenResponse githubTokenResponse = exchangeGithubToken(code, state);
        GithubUserInfoResponse githubUserInfoResponse = fetchGithubUserInfo(githubTokenResponse.getAccessToken());
        validateGithubUserInfo(githubUserInfoResponse);
        validateGithubAccountDuplication(githubUserInfoResponse);
        validateEmailDuplication(onboardingData.getEmail());

        User user = createGuestUser(onboardingData, githubUserInfoResponse);
        createOAuthAccounts(user, onboardingData, githubUserInfoResponse, githubTokenResponse.getAccessToken());

        String accessToken = jwtUtils.createAccessToken(user);
        String refreshToken = jwtUtils.createRefreshToken(user);
        saveRefreshToken(user.getUserId(), refreshToken);

        try {
            triggerGithubCollectAsync(user.getUserId(), githubTokenResponse.getAccessToken(), githubUserInfoResponse.getLogin());
        } catch (RuntimeException e) {
            log.warn("GitHub collect async 부가 트리거 처리 중 예외가 발생했습니다. userId={}", user.getUserId(), e);
        }
        deleteOnboardingData(googleSub);

        log.info("GitHub OAuth callback 완료. userId={}, githubLogin={}", user.getUserId(), githubUserInfoResponse.getLogin());
        return GithubAuthTokenBundle.builder()
                .response(GithubOAuthCallbackResponse.of(accessToken, user.getUserId()))
                .refreshToken(refreshToken)
                .build();
    }

    AuthTokenBundle handleExistingUser(OAuthAccount oAuthAccount, GoogleTokenResponse tokenResponse) {
        User user = oAuthAccount.getUser();
        updateProviderRefreshTokenIfPresent(oAuthAccount, tokenResponse.getRefreshToken());

        String accessToken = jwtUtils.createAccessToken(user);
        String refreshToken = jwtUtils.createRefreshToken(user);
        saveRefreshToken(user.getUserId(), refreshToken);

        log.info("Google OAuth 기존 유저 로그인 완료. userId={}", user.getUserId());
        return AuthTokenBundle.existingUser(
                GoogleOAuthCallbackResponse.forExistingUser(accessToken),
                refreshToken
        );
    }

    AuthTokenBundle handleNewUser(GoogleUserInfoResponse userInfoResponse) {
        String onboardingToken = jwtUtils.createOnboardingToken(userInfoResponse.getSub(), userInfoResponse.getEmail());
        saveOnboardingData(userInfoResponse);

        log.info("Google OAuth 신규 유저 확인 완료. googleSub={}", userInfoResponse.getSub());
        return AuthTokenBundle.newUser(
                GoogleOAuthCallbackResponse.forNewUser(
                        onboardingToken,
                        userInfoResponse.getEmail(),
                        userInfoResponse.getPicture()
                )
        );
    }

    GoogleTokenResponse exchangeGoogleToken(String code) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("code", code);
        formData.add("client_id", googleClientId);
        formData.add("client_secret", googleClientSecret);
        formData.add("redirect_uri", googleRedirectUri);
        formData.add("grant_type", oauthGrantType);

        try {
            GoogleTokenResponse response = RestClient.create()
                    .post()
                    .uri(googleTokenUrl)
                    .contentType(MediaType.parseMediaType(oauthContentType))
                    .body(formData)
                    .retrieve()
                    .body(GoogleTokenResponse.class);

            if (response == null || response.getAccessToken() == null || response.getAccessToken().isBlank()) {
                throw new GoogleTokenExchangeFailedException("Google access token 응답이 올바르지 않습니다.");
            }

            return response;
        } catch (RestClientException e) {
            throw new GoogleTokenExchangeFailedException("Google token 교환에 실패했습니다.", e);
        }
    }

    GoogleUserInfoResponse fetchGoogleUserInfo(String googleAccessToken) {
        try {
            GoogleUserInfoResponse response = RestClient.create()
                    .get()
                    .uri(googleUserInfoUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + googleAccessToken)
                    .retrieve()
                    .body(GoogleUserInfoResponse.class);

            if (response == null) {
                throw new GoogleUserInfoFetchFailedException("Google 사용자 정보 응답이 비어 있습니다.");
            }

            return response;
        } catch (RestClientException e) {
            throw new GoogleUserInfoFetchFailedException("Google 사용자 정보 조회에 실패했습니다.", e);
        }
    }

    GithubTokenResponse exchangeGithubToken(String code, String state) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("code", code);
        formData.add("client_id", githubClientId);
        formData.add("client_secret", githubClientSecret);
        formData.add("redirect_uri", githubRedirectUri);
        formData.add("state", state);

        try {
            GithubTokenResponse response = RestClient.create()
                    .post()
                    .uri(githubTokenUrl)
                    .contentType(MediaType.parseMediaType(oauthContentType))
                    .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                    .body(formData)
                    .retrieve()
                    .body(GithubTokenResponse.class);

            if (response == null || response.getAccessToken() == null || response.getAccessToken().isBlank()) {
                throw new GithubTokenExchangeFailedException("GitHub access token 응답이 올바르지 않습니다.");
            }

            return response;
        } catch (RestClientException e) {
            throw new GithubTokenExchangeFailedException("GitHub token 교환에 실패했습니다.", e);
        }
    }

    GithubUserInfoResponse fetchGithubUserInfo(String githubAccessToken) {
        try {
            GithubUserInfoResponse response = RestClient.create()
                    .get()
                    .uri(githubUserInfoUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + githubAccessToken)
                    .header(HttpHeaders.ACCEPT, githubAcceptVnd)
                    .header("X-GitHub-Api-Version", githubApiVersion)
                    .header(HttpHeaders.USER_AGENT, githubUserAgent)
                    .retrieve()
                    .body(GithubUserInfoResponse.class);

            if (response == null) {
                throw new GithubUserInfoFetchFailedException("GitHub 사용자 정보 응답이 비어 있습니다.");
            }

            return response;
        } catch (RestClientException e) {
            throw new GithubUserInfoFetchFailedException("GitHub 사용자 정보 조회에 실패했습니다.", e);
        }
    }

    void triggerGithubCollectAsync(Long userId, String githubAccessToken, String githubUsername) {
        GithubCollectAsyncRequest request = GithubCollectAsyncRequest.builder()
                .userId(userId)
                .githubToken(githubAccessToken)
                .githubUsername(githubUsername)
                .build();

        try {
            GithubCollectAsyncResponse response = AIRestClient.buildAiRestClient()
                    .post()
                    .uri(aiServerUrl + aiCollectAsyncPath)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(GithubCollectAsyncResponse.class);

            if (response != null && response.getTaskId() != null && !response.getTaskId().isBlank()) {
                log.info("GitHub collect async 트리거 성공. userId={}, taskId={}", userId, response.getTaskId());
                return;
            }

            log.warn("GitHub collect async 응답에 taskId가 없습니다. userId={}", userId);
        } catch (RestClientException e) {
            log.warn("GitHub collect async 트리거 실패. userId={}", userId, e);
        }
    }

    private void validateAuthorizationCode(String code) {
        if (code == null || code.isBlank()) {
            throw new GoogleAuthorizationCodeMissingException();
        }
    }

    private void validateGithubAuthorizationCode(String code) {
        if (code == null || code.isBlank()) {
            throw new GithubAuthorizationCodeMissingException();
        }
    }

    private Claims validateOnboardingToken(String authorizationHeader) {
        String onboardingToken = extractBearerToken(authorizationHeader);

        try {
            Claims claims = jwtUtils.getClaims(onboardingToken);
            validateOnboardingClaims(claims);
            return claims;
        } catch (JwtException | IllegalArgumentException e) {
            throw new InvalidOnboardingTokenException("유효하지 않은 onboarding token 입니다.", e);
        }
    }

    private Claims validateOnboardingTokenWithState(String state) {
        if (state == null || state.isBlank()) {
            throw new InvalidOnboardingTokenException("Authorization 헤더가 없습니다.");
        }
        String authorizationHeader = BEARER_PREFIX + state;
        return validateOnboardingToken(authorizationHeader);
    }

    private void validateGoogleUserInfo(GoogleUserInfoResponse userInfoResponse) {
        if (userInfoResponse.getSub() == null || userInfoResponse.getSub().isBlank()) {
            throw new GoogleUserInfoFetchFailedException("Google 사용자 식별값(sub)이 없습니다.");
        }

        if (userInfoResponse.getEmail() == null || userInfoResponse.getEmail().isBlank()) {
            throw new GoogleUserInfoFetchFailedException("Google 사용자 이메일 정보가 없습니다.");
        }
    }

    private void validateGithubUserInfo(GithubUserInfoResponse userInfoResponse) {
        if (userInfoResponse.getId() == null) {
            throw new GithubUserInfoFetchFailedException("GitHub 사용자 식별값(id)이 없습니다.");
        }

        if (userInfoResponse.getLogin() == null || userInfoResponse.getLogin().isBlank()) {
            throw new GithubUserInfoFetchFailedException("GitHub 사용자 로그인 정보가 없습니다.");
        }
    }

    private void validateGithubAccountDuplication(GithubUserInfoResponse userInfoResponse) {
        String providerAccountId = String.valueOf(userInfoResponse.getId());
        if (oAuthAccountRepository.findByProviderAndProviderAccountId(OAuthProvider.GITHUB, providerAccountId).isPresent()) {
            throw new DuplicateGithubAccountException(userInfoResponse.getLogin());
        }
    }

    private void validateEmailDuplication(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateOAuthEmailException(email);
        }
    }

    private void updateProviderRefreshTokenIfPresent(OAuthAccount oAuthAccount, String providerRefreshToken) {
        if (providerRefreshToken != null && !providerRefreshToken.isBlank()) {
            oAuthAccount.updateRefreshToken(providerRefreshToken);
        }
    }

    private String extractBearerToken(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            throw new InvalidOnboardingTokenException("Authorization 헤더가 없습니다.");
        }

        if (!authorizationHeader.startsWith(BEARER_PREFIX)) {
            throw new InvalidOnboardingTokenException("Authorization 헤더는 Bearer 형식이어야 합니다.");
        }

        String token = authorizationHeader.substring(BEARER_PREFIX.length()).trim();
        if (token.isBlank()) {
            throw new InvalidOnboardingTokenException("onboarding token 이 없습니다.");
        }

        return token;
    }

    private void validateOnboardingClaims(Claims claims) {
        if (!ONBOARDING_TOKEN_SUBJECT.equals(claims.getSubject())) {
            throw new InvalidOnboardingTokenException("onboarding token subject가 올바르지 않습니다.");
        }

        String purpose = claims.get("purpose", String.class);
        if (!ONBOARDING_TOKEN_PURPOSE.equals(purpose)) {
            throw new InvalidOnboardingTokenException("onboarding token purpose가 올바르지 않습니다.");
        }
    }

    private String extractGoogleSub(Claims claims) {
        String googleSub = claims.get("googleSub", String.class);
        if (googleSub == null || googleSub.isBlank()) {
            throw new InvalidOnboardingTokenException("onboarding token에 googleSub가 없습니다.");
        }

        return googleSub;
    }

    private void validateOnboardingRedisState(String googleSub) {
        String onboardingKey = ONBOARDING_REDIS_KEY_PREFIX + googleSub;
        if (!redisService.hasKey(onboardingKey)) {
            throw new InvalidOnboardingTokenException("Redis에 onboarding 정보가 없습니다.");
        }
    }

    private OnboardingData findOnboardingData(String googleSub) {
        validateOnboardingRedisState(googleSub);

        String payload = redisService.get(ONBOARDING_REDIS_KEY_PREFIX + googleSub);
        if (payload == null || payload.isBlank()) {
            throw new InvalidOnboardingTokenException("Redis에 onboarding 정보가 없습니다.");
        }

        OnboardingData onboardingData = parseOnboardingData(payload);
        if (onboardingData.getEmail() == null || onboardingData.getEmail().isBlank()) {
            throw new InvalidOnboardingTokenException("onboarding 데이터에 email이 없습니다.");
        }
        if (onboardingData.getGoogleSub() == null || onboardingData.getGoogleSub().isBlank()) {
            throw new InvalidOnboardingTokenException("onboarding 데이터에 googleSub가 없습니다.");
        }
        return onboardingData;
    }

    private void saveOnboardingData(GoogleUserInfoResponse userInfoResponse) {
        try {
            redisService.save(
                    ONBOARDING_REDIS_KEY_PREFIX + userInfoResponse.getSub(),
                    buildOnboardingPayload(userInfoResponse),
                    ONBOARDING_TOKEN_TTL_SECONDS,
                    TimeUnit.SECONDS
            );
        } catch (RuntimeException e) {
            throw new AuthRedisSaveFailedException("신규 유저 onboarding 정보 저장에 실패했습니다.", e);
        }
    }

    private User createGuestUser(OnboardingData onboardingData, GithubUserInfoResponse githubUserInfoResponse) {
        User user = User.builder()
                .email(onboardingData.getEmail())
                .nickname(buildNickname(onboardingData, githubUserInfoResponse))
                .profileImageUrl(onboardingData.getProfileImageUrl())
                .status(UserStatus.GUEST)
                .build();

        try {
            return userRepository.saveAndFlush(user);
        } catch (RuntimeException e) {
            throw new AuthPersistenceException("User 저장에 실패했습니다.", e);
        }
    }

    private void createOAuthAccounts(
            User user,
            OnboardingData onboardingData,
            GithubUserInfoResponse githubUserInfoResponse,
            String githubAccessToken) {
        String encryptedGithubAccessToken = oAuthTokenCryptoService.encrypt(githubAccessToken);

        OAuthAccount googleAccount = OAuthAccount.builder()
                .user(user)
                .provider(OAuthProvider.GOOGLE)
                .providerAccountId(onboardingData.getGoogleSub())
                .build();
        OAuthAccount githubAccount = OAuthAccount.builder()
                .user(user)
                .provider(OAuthProvider.GITHUB)
                .providerAccountId(String.valueOf(githubUserInfoResponse.getId()))
                .refreshToken(encryptedGithubAccessToken)
                .build();

        try {
            oAuthAccountRepository.save(googleAccount);
            oAuthAccountRepository.save(githubAccount);
        } catch (RuntimeException e) {
            throw new AuthPersistenceException("OAuthAccount 저장에 실패했습니다.", e);
        }
    }

    private void saveRefreshToken(Long userId, String refreshToken) {
        try {
            redisService.save(
                    "auth:refresh:" + userId,
                    refreshToken,
                    refreshTokenDurationTime,
                    TimeUnit.HOURS
            );
        } catch (RuntimeException e) {
            throw new AuthRedisSaveFailedException("refresh token 저장에 실패했습니다.", e);
        }
    }

    private void deleteOnboardingData(String googleSub) {
        String key = ONBOARDING_REDIS_KEY_PREFIX + googleSub;
        if (!redisService.delete(key)) {
            log.warn("onboarding Redis key 삭제 실패 또는 키 없음. key={}", key);
        }
    }

    private String buildOnboardingPayload(GoogleUserInfoResponse userInfoResponse) {
        return "{\"email\":\"" + escapeJson(userInfoResponse.getEmail()) + "\","
                + "\"profileImageUrl\":\"" + escapeJson(userInfoResponse.getPicture()) + "\","
                + "\"googleSub\":\"" + escapeJson(userInfoResponse.getSub()) + "\"}";
    }

    private String buildNickname(OnboardingData onboardingData, GithubUserInfoResponse githubUserInfoResponse) {
        if (githubUserInfoResponse.getLogin() != null && !githubUserInfoResponse.getLogin().isBlank()) {
            return githubUserInfoResponse.getLogin();
        }

        String email = onboardingData.getEmail();
        int separatorIndex = email.indexOf("@");
        if (separatorIndex > 0) {
            return email.substring(0, separatorIndex);
        }

        return email;
    }

    private OnboardingData parseOnboardingData(String payload) {
        String email = extractJsonValue(payload, "email");
        String profileImageUrl = extractJsonValue(payload, "profileImageUrl");
        String googleSub = extractJsonValue(payload, "googleSub");

        if (email == null && profileImageUrl == null && googleSub == null) {
            throw new InvalidOnboardingTokenException("onboarding 데이터 파싱에 실패했습니다.");
        }

        return new OnboardingData(email, profileImageUrl, googleSub);
    }

    private String extractJsonValue(String payload, String key) {
        String marker = "\"" + key + "\":\"";
        int startIndex = payload.indexOf(marker);
        if (startIndex < 0) {
            return null;
        }

        int valueStartIndex = startIndex + marker.length();
        int valueEndIndex = payload.indexOf("\"", valueStartIndex);
        if (valueEndIndex < 0) {
            throw new InvalidOnboardingTokenException("onboarding 데이터 파싱에 실패했습니다.");
        }

        return payload.substring(valueStartIndex, valueEndIndex)
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }

        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    @lombok.Getter
    @lombok.RequiredArgsConstructor
    private static class OnboardingData {
        private final String email;
        private final String profileImageUrl;
        private final String googleSub;
    }
}
