package com.ssafy.springbootbe.domain.auth.service;

import com.ssafy.springbootbe.common.jwt.JWTUtils;
import com.ssafy.springbootbe.common.redis.RedisService;
import com.ssafy.springbootbe.domain.auth.dto.response.AuthTokenBundle;
import com.ssafy.springbootbe.domain.auth.dto.response.GoogleTokenResponse;
import com.ssafy.springbootbe.domain.auth.dto.response.GoogleUserInfoResponse;
import com.ssafy.springbootbe.domain.auth.exception.AuthRedisSaveFailedException;
import com.ssafy.springbootbe.domain.auth.exception.DuplicateOAuthEmailException;
import com.ssafy.springbootbe.domain.auth.exception.GoogleTokenExchangeFailedException;
import com.ssafy.springbootbe.domain.auth.exception.GoogleUserInfoFetchFailedException;
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

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        authService = spy(new AuthServiceImpl(
                oAuthAccountRepository,
                userRepository,
                redisService,
                jwtUtils
        ));

        ReflectionTestUtils.setField(authService, "refreshTokenDurationTime", 168L);
    }

    @Test
    void 구글_콜백_신규_유저_성공() {
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

        // when
        AuthTokenBundle result = authService.handleGoogleCallback("valid-code");

        // then
        assertThat(result.getResponse().getIsNewUser()).isTrue();
        assertThat(result.getResponse().getOnboardingToken()).isEqualTo("onboarding-token");
        assertThat(result.getResponse().getEmail()).isEqualTo("new-user@gmail.com");
        verify(redisService).save(
                org.mockito.ArgumentMatchers.eq("onboarding:google-sub"),
                anyString(),
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

        // when
        AuthTokenBundle result = authService.handleGoogleCallback("valid-code");

        // then
        assertThat(result.getResponse().getIsNewUser()).isFalse();
        assertThat(result.getResponse().getAccessToken()).isEqualTo("service-access-token");
        assertThat(result.getRefreshToken()).isEqualTo("service-refresh-token");
        assertThat(oAuthAccount.getRefreshToken()).isEqualTo("new-provider-refresh");
        verify(redisService).save(
                org.mockito.ArgumentMatchers.eq("auth:refresh:1"),
                anyString(),
                org.mockito.ArgumentMatchers.eq(168L),
                org.mockito.ArgumentMatchers.eq(TimeUnit.HOURS)
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
        assertThatThrownBy(() -> authService.handleGoogleCallback("dup-code"))
                .isInstanceOf(DuplicateOAuthEmailException.class);
    }

    @Test
    void 구글_콜백_토큰_교환_실패() {
        // given
        doThrow(new GoogleTokenExchangeFailedException("Google token 교환에 실패했습니다."))
                .when(authService).exchangeGoogleToken("bad-code");

        // when & then
        assertThatThrownBy(() -> authService.handleGoogleCallback("bad-code"))
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
        assertThatThrownBy(() -> authService.handleGoogleCallback("bad-userinfo"))
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
        assertThatThrownBy(() -> authService.handleGoogleCallback("valid-code"))
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
                .save(org.mockito.ArgumentMatchers.eq("auth:refresh:1"), anyString(), org.mockito.ArgumentMatchers.eq(168L), org.mockito.ArgumentMatchers.eq(TimeUnit.HOURS));

        // when & then
        assertThatThrownBy(() -> authService.handleGoogleCallback("valid-code"))
                .isInstanceOf(AuthRedisSaveFailedException.class);
    }
}
