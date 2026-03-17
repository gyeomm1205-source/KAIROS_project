package com.ssafy.springbootbe.domain.auth.service;

import com.ssafy.springbootbe.common.jwt.JWTUtils;
import com.ssafy.springbootbe.common.redis.RedisService;
import com.ssafy.springbootbe.domain.auth.dto.response.AuthTokenBundle;
import com.ssafy.springbootbe.domain.auth.dto.response.GoogleOAuthCallbackResponse;
import com.ssafy.springbootbe.domain.auth.dto.response.GoogleTokenResponse;
import com.ssafy.springbootbe.domain.auth.dto.response.GoogleUserInfoResponse;
import com.ssafy.springbootbe.domain.auth.exception.AuthRedisSaveFailedException;
import com.ssafy.springbootbe.domain.auth.exception.DuplicateOAuthEmailException;
import com.ssafy.springbootbe.domain.auth.exception.GithubRedirectGenerationException;
import com.ssafy.springbootbe.domain.auth.exception.GoogleAuthorizationCodeMissingException;
import com.ssafy.springbootbe.domain.auth.exception.GoogleTokenExchangeFailedException;
import com.ssafy.springbootbe.domain.auth.exception.GoogleUserInfoFetchFailedException;
import com.ssafy.springbootbe.domain.auth.exception.InvalidOnboardingTokenException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import com.ssafy.springbootbe.persistence.oauth.entity.OAuthAccount;
import com.ssafy.springbootbe.persistence.oauth.repository.OAuthAccountRepository;
import com.ssafy.springbootbe.persistence.oauth.type.OAuthProvider;
import com.ssafy.springbootbe.persistence.user.entity.User;
import com.ssafy.springbootbe.persistence.user.repository.UserRepository;
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

    @Value("${oauth.content-type}")
    private String oauthContentType;

    @Value("${oauth.grant_type}")
    private String oauthGrantType;

    @Value("${service.refresh-token-duration}")
    private Long refreshTokenDurationTime;

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

    private void validateAuthorizationCode(String code) {
        if (code == null || code.isBlank()) {
            throw new GoogleAuthorizationCodeMissingException();
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

    private void validateGoogleUserInfo(GoogleUserInfoResponse userInfoResponse) {
        if (userInfoResponse.getSub() == null || userInfoResponse.getSub().isBlank()) {
            throw new GoogleUserInfoFetchFailedException("Google 사용자 식별값(sub)이 없습니다.");
        }

        if (userInfoResponse.getEmail() == null || userInfoResponse.getEmail().isBlank()) {
            throw new GoogleUserInfoFetchFailedException("Google 사용자 이메일 정보가 없습니다.");
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

    private String buildOnboardingPayload(GoogleUserInfoResponse userInfoResponse) {
        return "{\"email\":\"" + escapeJson(userInfoResponse.getEmail()) + "\","
                + "\"profileImageUrl\":\"" + escapeJson(userInfoResponse.getPicture()) + "\","
                + "\"googleSub\":\"" + escapeJson(userInfoResponse.getSub()) + "\"}";
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }

        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
