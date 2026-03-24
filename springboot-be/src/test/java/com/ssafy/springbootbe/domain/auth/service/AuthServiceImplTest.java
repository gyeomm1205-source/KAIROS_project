package com.ssafy.springbootbe.domain.auth.service;

import org.junit.jupiter.api.Disabled;
import com.ssafy.springbootbe.common.jwt.JWTUtils;
import com.ssafy.springbootbe.common.redis.RedisService;
import com.ssafy.springbootbe.common.utils.AIRestClient;
import com.ssafy.springbootbe.common.utils.OAuthTokenCryptoService;
import com.ssafy.springbootbe.domain.auth.dto.request.LinkGithubRequest;
import com.ssafy.springbootbe.domain.auth.dto.request.LinkVelogRequest;
import com.ssafy.springbootbe.domain.auth.dto.response.AuthTokenBundle;
import com.ssafy.springbootbe.domain.auth.dto.response.AuthReissueTokenBundle;
import com.ssafy.springbootbe.domain.auth.dto.response.GithubAuthTokenBundle;
import com.ssafy.springbootbe.domain.auth.dto.response.GithubTokenResponse;
import com.ssafy.springbootbe.domain.auth.dto.response.GithubUserInfoResponse;
import com.ssafy.springbootbe.domain.auth.dto.response.LinkVelogResponse;
import com.ssafy.springbootbe.domain.auth.exception.AlreadyUsedRefreshTokenException;
import com.ssafy.springbootbe.domain.auth.exception.DuplicateGithubAccountException;
import com.ssafy.springbootbe.domain.auth.dto.response.GoogleTokenResponse;
import com.ssafy.springbootbe.domain.auth.dto.response.GoogleUserInfoResponse;
import com.ssafy.springbootbe.domain.auth.exception.AuthTokenGenerationException;
import com.ssafy.springbootbe.domain.auth.exception.AuthRedisSaveFailedException;
import com.ssafy.springbootbe.domain.auth.exception.GithubAuthorizationCodeMissingException;
import com.ssafy.springbootbe.domain.auth.exception.GithubTokenExchangeFailedException;
import com.ssafy.springbootbe.domain.auth.exception.GithubUserInfoFetchFailedException;
import com.ssafy.springbootbe.domain.auth.exception.DuplicateOAuthEmailException;
import com.ssafy.springbootbe.domain.auth.exception.GoogleTokenExchangeFailedException;
import com.ssafy.springbootbe.domain.auth.exception.GoogleUserInfoFetchFailedException;
import com.ssafy.springbootbe.domain.auth.exception.InvalidAccessTokenException;
import com.ssafy.springbootbe.domain.auth.exception.InvalidRefreshTokenException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import com.ssafy.springbootbe.domain.auth.exception.InvalidOnboardingTokenException;
import com.ssafy.springbootbe.domain.auth.exception.VelogCollectAsyncFailedException;
import com.ssafy.springbootbe.persistence.oauth.entity.OAuthAccount;
import com.ssafy.springbootbe.persistence.oauth.repository.OAuthAccountRepository;
import com.ssafy.springbootbe.persistence.oauth.type.OAuthProvider;
import com.ssafy.springbootbe.persistence.user.entity.User;
import com.ssafy.springbootbe.persistence.user.repository.UserRepository;
import com.ssafy.springbootbe.persistence.user.type.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.ObjectMapper;

import java.util.Date;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private OAuthAccountRepository oAuthAccountRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RedisService redisService;

    @Mock
    private JWTUtils jwtUtils;

    @Mock
    private OAuthTokenCryptoService oAuthTokenCryptoService;

    @Mock
    private AIRestClient aiRestClient;

    private ObjectMapper objectMapper;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        authService = spy(new AuthServiceImpl(
                oAuthAccountRepository,
                userRepository,
                redisService,
                jwtUtils,
                oAuthTokenCryptoService,
                aiRestClient,
                objectMapper
        ));

        ReflectionTestUtils.setField(authService, "refreshTokenDurationTime", 168L);
        ReflectionTestUtils.setField(authService, "githubClientId", "test-github-client-id");
        ReflectionTestUtils.setField(authService, "githubRedirectUri", "http://localhost:5173/github/redirect");
        ReflectionTestUtils.setField(authService, "githubClientSecret", "test-github-client-secret");
        ReflectionTestUtils.setField(authService, "githubTokenUrl", "https://github.com/login/oauth/access_token");
        ReflectionTestUtils.setField(authService, "githubAcceptVnd", "application/vnd.github+json");
        ReflectionTestUtils.setField(authService, "githubApiVersion", "2026-03-10");
        ReflectionTestUtils.setField(authService, "githubUserInfoUrl", "https://api.github.com/user");
        ReflectionTestUtils.setField(authService, "githubUserAgent", "Kairos_Github_App");
        ReflectionTestUtils.setField(authService, "oauthContentType", "application/x-www-form-urlencoded");
        ReflectionTestUtils.setField(authService, "aiServerUrl", "http://localhost:8000");
        ReflectionTestUtils.setField(authService, "aiCollectAsyncPath", "/api/v1/ai/github/collect-async");
    }

    @Test
    void 구글_콜백_신규_유저_성공() {
        // given
        GoogleTokenResponse tokenResponse = GoogleTokenResponse.builder()
                .accessToken("google-access-token")
                .refreshToken("google-at-for-storage")
                .build();
        GoogleUserInfoResponse userInfoResponse = GoogleUserInfoResponse.builder()
                .sub("google-sub")
                .email("new-user@gmail.com")
                .picture("https://image.example/profile.png")
                .build();

        doReturn(tokenResponse).when(authService).exchangeGoogleToken("valid-code");
        doReturn(userInfoResponse).when(authService).fetchGoogleUserInfo("google-access-token");
        given(oAuthAccountRepository.findByProviderAndProviderAccountId(OAuthProvider.GOOGLE, "google-sub"))
                .willReturn(Optional.empty());
        given(userRepository.findByEmail("new-user@gmail.com")).willReturn(Optional.empty());
        given(jwtUtils.createOnboardingToken("google-sub", "new-user@gmail.com")).willReturn("onboarding-token");

        // when
        AuthTokenBundle result = authService.loginWithGoogle("valid-code");

        // then
        assertThat(result.getResponse().getIsNewUser()).isTrue();
        assertThat(result.getResponse().getOnboardingToken()).isEqualTo("onboarding-token");
        assertThat(result.getResponse().getEmail()).isEqualTo("new-user@gmail.com");
        verify(redisService).save(
                org.mockito.ArgumentMatchers.eq("onboarding:google-sub"),
                org.mockito.ArgumentMatchers.argThat(payload ->
                        payload.contains("googleAccessToken") && payload.contains("google-at-for-storage")),
                org.mockito.ArgumentMatchers.eq(1800L),
                org.mockito.ArgumentMatchers.eq(TimeUnit.SECONDS)
        );
    }

    @Test
    void 구글_콜백_기존_유저_성공() {
        // given
        User user = User.builder()
                .userId(1L)
                .email("existing@gmail.com")
                .nickname("existing")
                .status(UserStatus.ACTIVE)
                .build();
        OAuthAccount oAuthAccount = OAuthAccount.builder()
                .oauthAccountId(1L)
                .user(user)
                .provider(OAuthProvider.GOOGLE)
                .providerAccountId("google-sub")
                .refreshToken("old-provider-refresh")
                .build();
        GoogleTokenResponse tokenResponse = GoogleTokenResponse.builder()
                .accessToken("google-access-token")
                .refreshToken("new-provider-refresh")
                .build();
        GoogleUserInfoResponse userInfoResponse = GoogleUserInfoResponse.builder()
                .sub("google-sub")
                .email("existing@gmail.com")
                .picture("https://image.example/profile.png")
                .build();

        doReturn(tokenResponse).when(authService).exchangeGoogleToken("valid-code");
        doReturn(userInfoResponse).when(authService).fetchGoogleUserInfo("google-access-token");
        given(oAuthAccountRepository.findByProviderAndProviderAccountId(OAuthProvider.GOOGLE, "google-sub"))
                .willReturn(Optional.of(oAuthAccount));
        given(jwtUtils.createAccessToken(user)).willReturn("service-access-token");
        given(jwtUtils.createRefreshToken(user)).willReturn("service-refresh-token");
        given(oAuthTokenCryptoService.encrypt("new-provider-refresh")).willReturn("encrypted-new-provider-refresh");

        // when
        AuthTokenBundle result = authService.loginWithGoogle("valid-code");

        // then
        assertThat(result.getResponse().getIsNewUser()).isFalse();
        assertThat(result.getResponse().getAccessToken()).isEqualTo("service-access-token");
        assertThat(result.getRefreshToken()).isEqualTo("service-refresh-token");
        assertThat(oAuthAccount.getRefreshToken()).isEqualTo("encrypted-new-provider-refresh");
        verify(redisService).save(
                org.mockito.ArgumentMatchers.eq("refreshToken:1"),
                anyString(),
                org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.eq(TimeUnit.DAYS)
        );
        verify(userRepository, never()).findByEmail(anyString());
    }

    @Test
    void 구글_콜백_동일_이메일_중복_실패() {
        // given
        User existingUser = User.builder()
                .userId(3L)
                .email("dup@gmail.com")
                .nickname("dup")
                .status(UserStatus.ACTIVE)
                .build();
        GoogleTokenResponse tokenResponse = GoogleTokenResponse.builder()
                .accessToken("google-access-token")
                .build();
        GoogleUserInfoResponse userInfoResponse = GoogleUserInfoResponse.builder()
                .sub("google-sub")
                .email("dup@gmail.com")
                .picture("https://image.example/profile.png")
                .build();

        doReturn(tokenResponse).when(authService).exchangeGoogleToken("dup-code");
        doReturn(userInfoResponse).when(authService).fetchGoogleUserInfo("google-access-token");
        given(oAuthAccountRepository.findByProviderAndProviderAccountId(OAuthProvider.GOOGLE, "google-sub"))
                .willReturn(Optional.empty());
        given(userRepository.findByEmail("dup@gmail.com")).willReturn(Optional.of(existingUser));

        // when & then
        assertThatThrownBy(() -> authService.loginWithGoogle("dup-code"))
                .isInstanceOf(DuplicateOAuthEmailException.class);
    }

    @Test
    void 구글_콜백_토큰_교환_실패() {
        // given
        doThrow(new GoogleTokenExchangeFailedException("Google token 교환에 실패했습니다."))
                .when(authService).exchangeGoogleToken("bad-code");

        // when & then
        assertThatThrownBy(() -> authService.loginWithGoogle("bad-code"))
                .isInstanceOf(GoogleTokenExchangeFailedException.class);
    }

    @Test
    void 구글_콜백_사용자정보_조회_실패() {
        // given
        GoogleTokenResponse tokenResponse = GoogleTokenResponse.builder()
                .accessToken("google-access-token")
                .build();
        doReturn(tokenResponse).when(authService).exchangeGoogleToken("bad-userinfo");
        doThrow(new GoogleUserInfoFetchFailedException("Google 사용자 정보 조회에 실패했습니다."))
                .when(authService).fetchGoogleUserInfo("google-access-token");

        // when & then
        assertThatThrownBy(() -> authService.loginWithGoogle("bad-userinfo"))
                .isInstanceOf(GoogleUserInfoFetchFailedException.class);
    }

    @Test
    void 구글_콜백_신규_유저_Redis_저장_실패() {
        // given
        GoogleTokenResponse tokenResponse = GoogleTokenResponse.builder()
                .accessToken("google-access-token")
                .build();
        GoogleUserInfoResponse userInfoResponse = GoogleUserInfoResponse.builder()
                .sub("google-sub")
                .email("new-user@gmail.com")
                .picture("https://image.example/profile.png")
                .build();

        doReturn(tokenResponse).when(authService).exchangeGoogleToken("valid-code");
        doReturn(userInfoResponse).when(authService).fetchGoogleUserInfo("google-access-token");
        given(oAuthAccountRepository.findByProviderAndProviderAccountId(OAuthProvider.GOOGLE, "google-sub"))
                .willReturn(Optional.empty());
        given(userRepository.findByEmail("new-user@gmail.com")).willReturn(Optional.empty());
        given(jwtUtils.createOnboardingToken("google-sub", "new-user@gmail.com")).willReturn("onboarding-token");
        doThrow(new RuntimeException("redis error")).when(redisService)
                .save(org.mockito.ArgumentMatchers.eq("onboarding:google-sub"), anyString(), org.mockito.ArgumentMatchers.eq(1800L), org.mockito.ArgumentMatchers.eq(TimeUnit.SECONDS));

        // when & then
        assertThatThrownBy(() -> authService.loginWithGoogle("valid-code"))
                .isInstanceOf(AuthRedisSaveFailedException.class);
    }

    @Test
    void 구글_콜백_기존_유저_refreshToken_Redis_저장_실패() {
        // given
        User user = User.builder()
                .userId(1L)
                .email("existing@gmail.com")
                .nickname("existing")
                .status(UserStatus.ACTIVE)
                .build();
        OAuthAccount oAuthAccount = OAuthAccount.builder()
                .oauthAccountId(1L)
                .user(user)
                .provider(OAuthProvider.GOOGLE)
                .providerAccountId("google-sub")
                .build();
        GoogleTokenResponse tokenResponse = GoogleTokenResponse.builder()
                .accessToken("google-access-token")
                .build();
        GoogleUserInfoResponse userInfoResponse = GoogleUserInfoResponse.builder()
                .sub("google-sub")
                .email("existing@gmail.com")
                .picture("https://image.example/profile.png")
                .build();

        doReturn(tokenResponse).when(authService).exchangeGoogleToken("valid-code");
        doReturn(userInfoResponse).when(authService).fetchGoogleUserInfo("google-access-token");
        given(oAuthAccountRepository.findByProviderAndProviderAccountId(OAuthProvider.GOOGLE, "google-sub"))
                .willReturn(Optional.of(oAuthAccount));
        given(jwtUtils.createAccessToken(user)).willReturn("service-access-token");
        given(jwtUtils.createRefreshToken(user)).willReturn("service-refresh-token");
        doThrow(new RuntimeException("redis error")).when(redisService)
                .save(org.mockito.ArgumentMatchers.eq("refreshToken:1"), anyString(), org.mockito.ArgumentMatchers.eq(7L), org.mockito.ArgumentMatchers.eq(TimeUnit.DAYS));

        // when & then
        assertThatThrownBy(() -> authService.loginWithGoogle("valid-code"))
                .isInstanceOf(AuthRedisSaveFailedException.class);
    }

    @Test
    void 깃허브_콜백_신규_GUEST_유저_생성_성공() {
        // given
        Claims claims = org.mockito.Mockito.mock(Claims.class);
        User savedUser = User.builder()
                .userId(11L)
                .email("new-user@gmail.com")
                .nickname("github-login")
                .status(UserStatus.GUEST)
                .build();
        GithubTokenResponse githubTokenResponse = GithubTokenResponse.builder()
                .accessToken("github-access-token")
                .tokenType("bearer")
                .build();
        GithubUserInfoResponse githubUserInfoResponse = GithubUserInfoResponse.builder()
                .id(321L)
                .login("github-login")
                .build();

        given(jwtUtils.getClaims("onboarding-token")).willReturn(claims);
        given(claims.getSubject()).willReturn("onboarding");
        given(claims.get("purpose", String.class)).willReturn("onboarding");
        given(claims.get("googleSub", String.class)).willReturn("google-sub");
        given(redisService.hasKey("onboarding:google-sub")).willReturn(true);
        given(redisService.get("onboarding:google-sub"))
                .willReturn("{\"email\":\"new-user@gmail.com\",\"profileImageUrl\":\"https://image.example/profile.png\",\"googleSub\":\"google-sub\",\"googleAccessToken\":\"google-at-for-storage\"}");
        doReturn(githubTokenResponse).when(authService).exchangeGithubToken("valid-code");
        doReturn(githubUserInfoResponse).when(authService).fetchGithubUserInfo("github-access-token");
        given(oAuthAccountRepository.findByProviderAndProviderAccountId(OAuthProvider.GITHUB, "321"))
                .willReturn(Optional.empty());
        given(userRepository.existsByEmail("new-user@gmail.com")).willReturn(false);
        given(userRepository.saveAndFlush(org.mockito.ArgumentMatchers.any(User.class))).willReturn(savedUser);
        given(jwtUtils.createAccessToken(savedUser)).willReturn("service-access-token");
        given(jwtUtils.createRefreshToken(savedUser)).willReturn("service-refresh-token");
        given(oAuthTokenCryptoService.encrypt("github-access-token")).willReturn("encrypted-github-access-token");
        given(oAuthTokenCryptoService.encrypt("google-at-for-storage")).willReturn("encrypted-google-at");
        doReturn("task_github_abc").when(authService)
                .triggerGithubCollectAsync(11L, "github-access-token", "github-login");

        // when
        GithubAuthTokenBundle result = authService.linkGithub(
                "Bearer onboarding-token",
                LinkGithubRequest.builder().code("valid-code").build()
        );

        // then
        assertThat(result.getResponse().getAccessToken()).isEqualTo("service-access-token");
        assertThat(result.getResponse().getTokenType()).isEqualTo("Bearer");
        assertThat(result.getResponse().getUserId()).isEqualTo(11L);
        assertThat(result.getResponse().getGithubTaskId()).isEqualTo("task_github_abc");
        assertThat(result.getRefreshToken()).isEqualTo("service-refresh-token");
        verify(oAuthAccountRepository).save(org.mockito.ArgumentMatchers.argThat(account ->
                account.getProvider() == OAuthProvider.GOOGLE
                        && "google-sub".equals(account.getProviderAccountId())
                        && "encrypted-google-at".equals(account.getRefreshToken())
        ));
        verify(oAuthAccountRepository).save(org.mockito.ArgumentMatchers.argThat(account ->
                account.getProvider() == OAuthProvider.GITHUB
                        && "321".equals(account.getProviderAccountId())
                        && "encrypted-github-access-token".equals(account.getRefreshToken())
        ));
        verify(redisService).save(
                org.mockito.ArgumentMatchers.eq("refreshToken:11"),
                org.mockito.ArgumentMatchers.eq("service-refresh-token"),
                org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.eq(TimeUnit.DAYS)
        );
        verify(authService).triggerGithubCollectAsync(11L, "github-access-token", "github-login");
        verify(redisService).delete("onboarding:google-sub");
    }

    @Test
    void 깃허브_콜백_Authorization_헤더_누락_실패() {
        // when & then
        assertThatThrownBy(() -> authService.linkGithub(
                null,
                LinkGithubRequest.builder().code("valid-code").build()
        ))
                .isInstanceOf(InvalidOnboardingTokenException.class)
                .hasMessageContaining("Authorization 헤더가 없습니다.");
    }

    @Test
    void 깃허브_콜백_인가코드_누락_실패() {
        // when & then
        assertThatThrownBy(() -> authService.linkGithub(
                "Bearer onboarding-token",
                LinkGithubRequest.builder().code(null).build()
        ))
                .isInstanceOf(GithubAuthorizationCodeMissingException.class);
    }

    @Test
    void 깃허브_콜백_Redis_onboarding_정보가_없으면_실패() {
        // given
        Claims claims = org.mockito.Mockito.mock(Claims.class);
        given(jwtUtils.getClaims("onboarding-token")).willReturn(claims);
        given(claims.getSubject()).willReturn("onboarding");
        given(claims.get("purpose", String.class)).willReturn("onboarding");
        given(claims.get("googleSub", String.class)).willReturn("google-sub");
        given(redisService.hasKey("onboarding:google-sub")).willReturn(false);

        // when & then
        assertThatThrownBy(() -> authService.linkGithub(
                "Bearer onboarding-token",
                LinkGithubRequest.builder().code("valid-code").build()
        ))
                .isInstanceOf(InvalidOnboardingTokenException.class)
                .hasMessageContaining("Redis에 onboarding 정보가 없습니다.");
    }

    @Test
    void 깃허브_콜백_토큰_교환_실패() {
        // given
        Claims claims = org.mockito.Mockito.mock(Claims.class);
        given(jwtUtils.getClaims("onboarding-token")).willReturn(claims);
        given(claims.getSubject()).willReturn("onboarding");
        given(claims.get("purpose", String.class)).willReturn("onboarding");
        given(claims.get("googleSub", String.class)).willReturn("google-sub");
        given(redisService.hasKey("onboarding:google-sub")).willReturn(true);
        given(redisService.get("onboarding:google-sub"))
                .willReturn("{\"email\":\"new-user@gmail.com\",\"profileImageUrl\":\"https://image.example/profile.png\",\"googleSub\":\"google-sub\"}");
        doThrow(new GithubTokenExchangeFailedException("GitHub token 교환에 실패했습니다."))
                .when(authService).exchangeGithubToken("valid-code");

        // when & then
        assertThatThrownBy(() -> authService.linkGithub(
                "Bearer onboarding-token",
                LinkGithubRequest.builder().code("valid-code").build()
        ))
                .isInstanceOf(GithubTokenExchangeFailedException.class);
    }

    @Test
    void 깃허브_콜백_사용자정보_조회_실패() {
        // given
        Claims claims = org.mockito.Mockito.mock(Claims.class);
        GithubTokenResponse githubTokenResponse = GithubTokenResponse.builder()
                .accessToken("github-access-token")
                .build();
        given(jwtUtils.getClaims("onboarding-token")).willReturn(claims);
        given(claims.getSubject()).willReturn("onboarding");
        given(claims.get("purpose", String.class)).willReturn("onboarding");
        given(claims.get("googleSub", String.class)).willReturn("google-sub");
        given(redisService.hasKey("onboarding:google-sub")).willReturn(true);
        given(redisService.get("onboarding:google-sub"))
                .willReturn("{\"email\":\"new-user@gmail.com\",\"profileImageUrl\":\"https://image.example/profile.png\",\"googleSub\":\"google-sub\"}");
        doReturn(githubTokenResponse).when(authService).exchangeGithubToken("valid-code");
        doThrow(new GithubUserInfoFetchFailedException("GitHub 사용자 정보 조회에 실패했습니다."))
                .when(authService).fetchGithubUserInfo("github-access-token");

        // when & then
        assertThatThrownBy(() -> authService.linkGithub(
                "Bearer onboarding-token",
                LinkGithubRequest.builder().code("valid-code").build()
        ))
                .isInstanceOf(GithubUserInfoFetchFailedException.class);
    }

    @Test
    void 깃허브_콜백_이미_가입된_GitHub_계정이면_실패() {
        // given
        Claims claims = org.mockito.Mockito.mock(Claims.class);
        GithubTokenResponse githubTokenResponse = GithubTokenResponse.builder()
                .accessToken("github-access-token")
                .build();
        GithubUserInfoResponse githubUserInfoResponse = GithubUserInfoResponse.builder()
                .id(321L)
                .login("github-login")
                .build();
        OAuthAccount githubAccount = OAuthAccount.builder()
                .oauthAccountId(77L)
                .provider(OAuthProvider.GITHUB)
                .providerAccountId("321")
                .build();

        given(jwtUtils.getClaims("onboarding-token")).willReturn(claims);
        given(claims.getSubject()).willReturn("onboarding");
        given(claims.get("purpose", String.class)).willReturn("onboarding");
        given(claims.get("googleSub", String.class)).willReturn("google-sub");
        given(redisService.hasKey("onboarding:google-sub")).willReturn(true);
        given(redisService.get("onboarding:google-sub"))
                .willReturn("{\"email\":\"new-user@gmail.com\",\"profileImageUrl\":\"https://image.example/profile.png\",\"googleSub\":\"google-sub\"}");
        doReturn(githubTokenResponse).when(authService).exchangeGithubToken("valid-code");
        doReturn(githubUserInfoResponse).when(authService).fetchGithubUserInfo("github-access-token");
        given(oAuthAccountRepository.findByProviderAndProviderAccountId(OAuthProvider.GITHUB, "321"))
                .willReturn(Optional.of(githubAccount));

        // when & then
        assertThatThrownBy(() -> authService.linkGithub(
                "Bearer onboarding-token",
                LinkGithubRequest.builder().code("valid-code").build()
        ))
                .isInstanceOf(DuplicateGithubAccountException.class);
    }

    @Test
    void 깃허브_콜백_service_token_발급_실패() {
        // given
        Claims claims = org.mockito.Mockito.mock(Claims.class);
        User savedUser = User.builder()
                .userId(11L)
                .email("new-user@gmail.com")
                .nickname("github-login")
                .status(UserStatus.GUEST)
                .build();
        GithubTokenResponse githubTokenResponse = GithubTokenResponse.builder()
                .accessToken("github-access-token")
                .build();
        GithubUserInfoResponse githubUserInfoResponse = GithubUserInfoResponse.builder()
                .id(321L)
                .login("github-login")
                .build();

        given(jwtUtils.getClaims("onboarding-token")).willReturn(claims);
        given(claims.getSubject()).willReturn("onboarding");
        given(claims.get("purpose", String.class)).willReturn("onboarding");
        given(claims.get("googleSub", String.class)).willReturn("google-sub");
        given(redisService.hasKey("onboarding:google-sub")).willReturn(true);
        given(redisService.get("onboarding:google-sub"))
                .willReturn("{\"email\":\"new-user@gmail.com\",\"profileImageUrl\":\"https://image.example/profile.png\",\"googleSub\":\"google-sub\"}");
        doReturn(githubTokenResponse).when(authService).exchangeGithubToken("valid-code");
        doReturn(githubUserInfoResponse).when(authService).fetchGithubUserInfo("github-access-token");
        given(oAuthAccountRepository.findByProviderAndProviderAccountId(OAuthProvider.GITHUB, "321"))
                .willReturn(Optional.empty());
        given(userRepository.existsByEmail("new-user@gmail.com")).willReturn(false);
        given(userRepository.saveAndFlush(org.mockito.ArgumentMatchers.any(User.class))).willReturn(savedUser);
        given(jwtUtils.createAccessToken(savedUser))
                .willThrow(new AuthTokenGenerationException("access token 발급에 실패했습니다.", new RuntimeException("jwt error")));
        given(oAuthTokenCryptoService.encrypt("github-access-token")).willReturn("github-access-token");

        // when & then
        assertThatThrownBy(() -> authService.linkGithub(
                "Bearer onboarding-token",
                LinkGithubRequest.builder().code("valid-code").build()
        ))
                .isInstanceOf(AuthTokenGenerationException.class);
    }

    @Test
    void 깃허브_콜백_FastAPI_호출_실패여도_회원생성은_성공() {
        // given
        Claims claims = org.mockito.Mockito.mock(Claims.class);
        User savedUser = User.builder()
                .userId(11L)
                .email("new-user@gmail.com")
                .nickname("github-login")
                .status(UserStatus.GUEST)
                .build();
        GithubTokenResponse githubTokenResponse = GithubTokenResponse.builder()
                .accessToken("github-access-token")
                .build();
        GithubUserInfoResponse githubUserInfoResponse = GithubUserInfoResponse.builder()
                .id(321L)
                .login("github-login")
                .build();

        given(jwtUtils.getClaims("onboarding-token")).willReturn(claims);
        given(claims.getSubject()).willReturn("onboarding");
        given(claims.get("purpose", String.class)).willReturn("onboarding");
        given(claims.get("googleSub", String.class)).willReturn("google-sub");
        given(redisService.hasKey("onboarding:google-sub")).willReturn(true);
        given(redisService.get("onboarding:google-sub"))
                .willReturn("{\"email\":\"new-user@gmail.com\",\"profileImageUrl\":\"https://image.example/profile.png\",\"googleSub\":\"google-sub\"}");
        doReturn(githubTokenResponse).when(authService).exchangeGithubToken("valid-code");
        doReturn(githubUserInfoResponse).when(authService).fetchGithubUserInfo("github-access-token");
        given(oAuthAccountRepository.findByProviderAndProviderAccountId(OAuthProvider.GITHUB, "321"))
                .willReturn(Optional.empty());
        given(userRepository.existsByEmail("new-user@gmail.com")).willReturn(false);
        given(userRepository.saveAndFlush(org.mockito.ArgumentMatchers.any(User.class))).willReturn(savedUser);
        given(jwtUtils.createAccessToken(savedUser)).willReturn("service-access-token");
        given(jwtUtils.createRefreshToken(savedUser)).willReturn("service-refresh-token");
        given(oAuthTokenCryptoService.encrypt("github-access-token")).willReturn("github-access-token");
        doThrow(new RestClientException("fastapi error"))
                .when(authService).triggerGithubCollectAsync(11L, "github-access-token", "github-login");

        // when
        GithubAuthTokenBundle result = authService.linkGithub(
                "Bearer onboarding-token",
                LinkGithubRequest.builder().code("valid-code").build()
        );

        // then
        assertThat(result.getResponse().getUserId()).isEqualTo(11L);
        assertThat(result.getResponse().getGithubTaskId()).isNull();
        assertThat(result.getRefreshToken()).isEqualTo("service-refresh-token");
        verify(redisService).delete("onboarding:google-sub");
    }

    @Test
    void 벨로그_연동_성공() {
        // given
        Claims claims = org.mockito.Mockito.mock(Claims.class);
        User user = User.builder()
                .userId(1L)
                .email("user@gmail.com")
                .nickname("tester")
                .status(UserStatus.ACTIVE)
                .build();
        LinkVelogRequest request = LinkVelogRequest.builder()
                .velogUsername(" teddynu ")
                .build();

        given(jwtUtils.getClaims("valid-access-token")).willReturn(claims);
        given(claims.getSubject()).willReturn("accessToken");
        given(claims.get("userId")).willReturn(1L);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(userRepository.saveAndFlush(user)).willReturn(user);
        doReturn("task_velog_xyz").when(authService).triggerProfileAnalysis(1L, "teddynu");

        // when
        LinkVelogResponse response = authService.linkVelog("Bearer valid-access-token", request);

        // then
        assertThat(response.getVelogUsername()).isEqualTo("teddynu");
        assertThat(response.getVelogTaskId()).isEqualTo("task_velog_xyz");
        assertThat(user.getVelogUsername()).isEqualTo("teddynu");
        verify(userRepository).saveAndFlush(user);
        verify(authService).triggerProfileAnalysis(1L, "teddynu");
    }

    @Test
    void 벨로그_연동_velogUsername_누락_실패() {
        // given
        LinkVelogRequest request = LinkVelogRequest.builder()
                .velogUsername(" ")
                .build();

        // when & then
        assertThatThrownBy(() -> authService.linkVelog("Bearer valid-access-token", request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("velogUsername은 필수입니다.");
    }

    @Test
    void 벨로그_연동_인증된_사용자가_없으면_실패() {
        // given
        Claims claims = org.mockito.Mockito.mock(Claims.class);
        LinkVelogRequest request = LinkVelogRequest.builder()
                .velogUsername("teddynu")
                .build();
        given(jwtUtils.getClaims("valid-access-token")).willReturn(claims);
        given(claims.getSubject()).willReturn("accessToken");
        given(claims.get("userId")).willReturn(1L);
        given(userRepository.findById(1L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> authService.linkVelog("Bearer valid-access-token", request))
                .isInstanceOf(InvalidAccessTokenException.class)
                .hasMessageContaining("access token에 해당하는 사용자가 없습니다.");
    }

    @Test
    void 벨로그_연동_FastAPI_호출_실패() {
        // given
        Claims claims = org.mockito.Mockito.mock(Claims.class);
        User user = User.builder()
                .userId(1L)
                .email("user@gmail.com")
                .nickname("tester")
                .status(UserStatus.ACTIVE)
                .build();
        LinkVelogRequest request = LinkVelogRequest.builder()
                .velogUsername("teddynu")
                .build();

        given(jwtUtils.getClaims("valid-access-token")).willReturn(claims);
        given(claims.getSubject()).willReturn("accessToken");
        given(claims.get("userId")).willReturn(1L);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(userRepository.saveAndFlush(user)).willReturn(user);
        doThrow(new VelogCollectAsyncFailedException("FastAPI Velog 수집 호출에 실패했습니다."))
                .when(authService).triggerProfileAnalysis(1L, "teddynu");

        // when & then
        assertThatThrownBy(() -> authService.linkVelog("Bearer valid-access-token", request))
                .isInstanceOf(VelogCollectAsyncFailedException.class)
                .hasMessageContaining("FastAPI Velog 수집 호출에 실패했습니다.");
        assertThat(user.getVelogUsername()).isEqualTo("teddynu");
        verify(userRepository).saveAndFlush(user);
    }

    @Test
    void 토큰_재발급_성공() {
        // given
        Claims claims = org.mockito.Mockito.mock(Claims.class);
        User user = User.builder()
                .userId(1L)
                .email("existing@gmail.com")
                .nickname("existing")
                .status(UserStatus.ACTIVE)
                .build();
        given(jwtUtils.getClaims("valid-refresh-token")).willReturn(claims);
        given(claims.getSubject()).willReturn("refreshToken");
        given(claims.get("userId")).willReturn(1L);
        given(redisService.get("refreshToken:1")).willReturn("valid-refresh-token");
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(jwtUtils.createAccessToken(user)).willReturn("new-access-token");
        given(jwtUtils.createRefreshToken(user)).willReturn("new-refresh-token");

        // when
        AuthReissueTokenBundle result = authService.reissueAccessToken("valid-refresh-token");

        // then
        assertThat(result.getResponse().getAccessToken()).isEqualTo("new-access-token");
        assertThat(result.getResponse().getTokenType()).isEqualTo("Bearer");
        assertThat(result.getRefreshToken()).isEqualTo("new-refresh-token");
        verify(redisService).save(
                org.mockito.ArgumentMatchers.eq("refreshToken:1"),
                org.mockito.ArgumentMatchers.eq("new-refresh-token"),
                org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.eq(TimeUnit.DAYS)
        );
    }

    @Test
    void 토큰_재발급_refresh_token_cookie_누락_실패() {
        // when & then
        assertThatThrownBy(() -> authService.reissueAccessToken(null))
                .isInstanceOf(InvalidRefreshTokenException.class)
                .hasMessageContaining("refresh token cookie가 없습니다.");
    }

    @Test
    void 토큰_재발급_만료된_refresh_token_실패() {
        // given
        given(jwtUtils.getClaims("expired-refresh-token"))
                .willThrow(new ExpiredJwtException(null, null, "expired"));

        // when & then
        assertThatThrownBy(() -> authService.reissueAccessToken("expired-refresh-token"))
                .isInstanceOf(InvalidRefreshTokenException.class)
                .hasMessageContaining("유효하지 않은 refresh token");
    }

    @Test
    void 토큰_재발급_Redis_저장값이_없으면_실패() {
        // given
        Claims claims = org.mockito.Mockito.mock(Claims.class);
        given(jwtUtils.getClaims("valid-refresh-token")).willReturn(claims);
        given(claims.getSubject()).willReturn("refreshToken");
        given(claims.get("userId")).willReturn(1L);
        given(redisService.get("refreshToken:1")).willReturn(null);

        // when & then
        assertThatThrownBy(() -> authService.reissueAccessToken("valid-refresh-token"))
                .isInstanceOf(InvalidRefreshTokenException.class)
                .hasMessageContaining("Redis에 refresh token 정보가 없습니다.");
    }

    @Test
    void 토큰_재발급_Redis_저장값과_불일치하면_RTR_위반_실패() {
        // given
        Claims claims = org.mockito.Mockito.mock(Claims.class);
        given(jwtUtils.getClaims("reused-refresh-token")).willReturn(claims);
        given(claims.getSubject()).willReturn("refreshToken");
        given(claims.get("userId")).willReturn(1L);
        given(redisService.get("refreshToken:1")).willReturn("latest-refresh-token");

        // when & then
        assertThatThrownBy(() -> authService.reissueAccessToken("reused-refresh-token"))
                .isInstanceOf(AlreadyUsedRefreshTokenException.class)
                .hasMessageContaining("이미 사용된 refresh token");
    }

    @Test
    void 토큰_재발급_refresh_token_Redis_갱신_실패() {
        // given
        Claims claims = org.mockito.Mockito.mock(Claims.class);
        User user = User.builder()
                .userId(1L)
                .email("existing@gmail.com")
                .nickname("existing")
                .status(UserStatus.ACTIVE)
                .build();
        given(jwtUtils.getClaims("valid-refresh-token")).willReturn(claims);
        given(claims.getSubject()).willReturn("refreshToken");
        given(claims.get("userId")).willReturn(1L);
        given(redisService.get("refreshToken:1")).willReturn("valid-refresh-token");
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(jwtUtils.createAccessToken(user)).willReturn("new-access-token");
        given(jwtUtils.createRefreshToken(user)).willReturn("new-refresh-token");
        doThrow(new RuntimeException("redis error")).when(redisService)
                .save(org.mockito.ArgumentMatchers.eq("refreshToken:1"), org.mockito.ArgumentMatchers.eq("new-refresh-token"), org.mockito.ArgumentMatchers.eq(7L), org.mockito.ArgumentMatchers.eq(TimeUnit.DAYS));

        // when & then
        assertThatThrownBy(() -> authService.reissueAccessToken("valid-refresh-token"))
                .isInstanceOf(AuthRedisSaveFailedException.class);
    }

    @Test
    void 로그아웃_성공() {
        // given
        Claims claims = org.mockito.Mockito.mock(Claims.class);
        long remainingMillis = 60_000L;
        Date expiration = new Date(System.currentTimeMillis() + remainingMillis);
        given(jwtUtils.getClaims("valid-access-token")).willReturn(claims);
        given(claims.getSubject()).willReturn("accessToken");
        given(claims.getExpiration()).willReturn(expiration);
        given(claims.get("userId")).willReturn(1L);

        // when
        authService.logout("Bearer valid-access-token");

        // then
        verify(redisService).save(
                org.mockito.ArgumentMatchers.eq("blacklist:valid-access-token"),
                org.mockito.ArgumentMatchers.eq("deleted"),
                org.mockito.ArgumentMatchers.longThat(ttl -> ttl > 0L && ttl <= remainingMillis),
                org.mockito.ArgumentMatchers.eq(TimeUnit.MILLISECONDS)
        );
        verify(redisService).delete("refreshToken:1");
    }

    @Test
    void 로그아웃_Authorization_헤더_누락_실패() {
        // when & then
        assertThatThrownBy(() -> authService.logout(null))
                .isInstanceOf(InvalidAccessTokenException.class)
                .hasMessageContaining("Authorization 헤더가 없습니다.");
    }

    @Test
    void 로그아웃_Bearer_형식_오류_실패() {
        // when & then
        assertThatThrownBy(() -> authService.logout("Token access-token"))
                .isInstanceOf(InvalidAccessTokenException.class)
                .hasMessageContaining("Bearer 형식");
    }

    @Test
    void 로그아웃_만료된_access_token_실패() {
        // given
        given(jwtUtils.getClaims("expired-access-token"))
                .willThrow(new ExpiredJwtException(null, null, "expired"));

        // when & then
        assertThatThrownBy(() -> authService.logout("Bearer expired-access-token"))
                .isInstanceOf(InvalidAccessTokenException.class)
                .hasMessageContaining("유효하지 않은 access token");
    }

    @Test
    void 로그아웃_subject가_accessToken이_아니면_실패() {
        // given
        Claims claims = org.mockito.Mockito.mock(Claims.class);
        given(jwtUtils.getClaims("wrong-subject-token")).willReturn(claims);
        given(claims.getSubject()).willReturn("refreshToken");

        // when & then
        assertThatThrownBy(() -> authService.logout("Bearer wrong-subject-token"))
                .isInstanceOf(InvalidAccessTokenException.class)
                .hasMessageContaining("subject");
    }

    @Test
    void 로그아웃_blacklist_Redis_저장_실패() {
        // given
        Claims claims = org.mockito.Mockito.mock(Claims.class);
        Date expiration = new Date(System.currentTimeMillis() + 60_000L);
        given(jwtUtils.getClaims("valid-access-token")).willReturn(claims);
        given(claims.getSubject()).willReturn("accessToken");
        given(claims.getExpiration()).willReturn(expiration);
        given(claims.get("userId")).willReturn(1L);
        doThrow(new RuntimeException("redis error")).when(redisService)
                .save(
                        org.mockito.ArgumentMatchers.eq("blacklist:valid-access-token"),
                        org.mockito.ArgumentMatchers.eq("deleted"),
                        org.mockito.ArgumentMatchers.anyLong(),
                        org.mockito.ArgumentMatchers.eq(TimeUnit.MILLISECONDS)
                );

        // when & then
        assertThatThrownBy(() -> authService.logout("Bearer valid-access-token"))
                .isInstanceOf(AuthRedisSaveFailedException.class)
                .hasMessageContaining("blacklist 저장");
    }

    @Test
    void 로그아웃_refresh_token_삭제_실패() {
        // given
        Claims claims = org.mockito.Mockito.mock(Claims.class);
        Date expiration = new Date(System.currentTimeMillis() + 60_000L);
        given(jwtUtils.getClaims("valid-access-token")).willReturn(claims);
        given(claims.getSubject()).willReturn("accessToken");
        given(claims.getExpiration()).willReturn(expiration);
        given(claims.get("userId")).willReturn(1L);
        doThrow(new RuntimeException("redis error")).when(redisService)
                .delete("refreshToken:1");

        // when & then
        assertThatThrownBy(() -> authService.logout("Bearer valid-access-token"))
                .isInstanceOf(AuthRedisSaveFailedException.class)
                .hasMessageContaining("refresh token 삭제");
    }
}
