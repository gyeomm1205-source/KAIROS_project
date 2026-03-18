package com.ssafy.springbootbe.domain.auth.controller;

import com.ssafy.springbootbe.domain.auth.dto.response.AuthReissueResponse;
import com.ssafy.springbootbe.domain.auth.dto.response.AuthReissueTokenBundle;
import com.ssafy.springbootbe.domain.auth.dto.response.AuthTokenBundle;
import com.ssafy.springbootbe.domain.auth.dto.response.GithubAuthTokenBundle;
import com.ssafy.springbootbe.domain.auth.dto.response.GithubOAuthCallbackResponse;
import com.ssafy.springbootbe.domain.auth.dto.response.GoogleOAuthCallbackResponse;
import com.ssafy.springbootbe.domain.auth.exception.AlreadyUsedRefreshTokenException;
import com.ssafy.springbootbe.domain.auth.exception.DuplicateGithubAccountException;
import com.ssafy.springbootbe.domain.auth.exception.GithubAuthorizationCodeMissingException;
import com.ssafy.springbootbe.domain.auth.exception.GithubTokenExchangeFailedException;
import com.ssafy.springbootbe.domain.auth.exception.DuplicateOAuthEmailException;
import com.ssafy.springbootbe.domain.auth.exception.GoogleTokenExchangeFailedException;
import com.ssafy.springbootbe.domain.auth.exception.GoogleUserInfoFetchFailedException;
import com.ssafy.springbootbe.domain.auth.exception.GithubUserInfoFetchFailedException;
import com.ssafy.springbootbe.domain.auth.exception.InvalidRefreshTokenException;
import com.ssafy.springbootbe.domain.auth.exception.InvalidOnboardingTokenException;
import com.ssafy.springbootbe.domain.auth.service.AuthService;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URI;
import jakarta.servlet.http.Cookie;

@WebMvcTest(
        value = AuthController.class,
        properties = {
                "oauth.google.auth_uri=https://accounts.google.com/o/oauth2/auth",
                "oauth.google.client_id=test-google-client-id",
                "oauth.google.redirect_uri=http://localhost:5173/google/redirect",
                "oauth.google.scope=openid email profile",
                "oauth.github.auth_uri=https://github.com/login/oauth/authorize",
                "oauth.github.client_id=test-github-client-id",
                "oauth.github.redirect_uri=http://localhost:5173/github/redirect",
                "oauth.github.scope=read:user repo"
        }
)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @Test
    void 구글_로그인_시작_리다이렉트_성공() throws Exception {
        // when & then
        mockMvc.perform(get("/auth/oauth2/google"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrlPattern("https://accounts.google.com/**"))
                .andExpect(header().string("Location", Matchers.containsString("client_id=test-google-client-id")))
                .andExpect(header().string("Location", Matchers.containsString("redirect_uri=http://localhost:5173/google/redirect")))
                .andExpect(header().string("Location", Matchers.containsString("response_type=code")))
                .andExpect(header().string("Location", Matchers.containsString("scope=openid%20email%20profile")));
    }

    @Test
    void 구글_콜백_신규_유저_성공() throws Exception {
        // given
        GoogleOAuthCallbackResponse response = GoogleOAuthCallbackResponse.forNewUser(
                "onboarding-token",
                "new-user@gmail.com",
                "https://image.example/profile.png"
        );
        given(authService.handleGoogleCallback("valid-code"))
                .willReturn(AuthTokenBundle.newUser(response));

        // when & then
        mockMvc.perform(get("/auth/oauth2/callback/google")
                        .param("code", "valid-code"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.is_new_user").value(true))
                .andExpect(jsonPath("$.onboarding_token").value("onboarding-token"))
                .andExpect(jsonPath("$.email").value("new-user@gmail.com"))
                .andExpect(jsonPath("$.profile_image_url").value("https://image.example/profile.png"))
                .andExpect(header().doesNotExist("Set-Cookie"));
    }

    @Test
    void 구글_콜백_기존_유저_성공() throws Exception {
        // given
        GoogleOAuthCallbackResponse response = GoogleOAuthCallbackResponse.forExistingUser("access-token");
        given(authService.handleGoogleCallback("valid-code"))
                .willReturn(AuthTokenBundle.existingUser(response, "refresh-token"));

        // when & then
        mockMvc.perform(get("/auth/oauth2/callback/google")
                        .param("code", "valid-code"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.is_new_user").value(false))
                .andExpect(jsonPath("$.access_token").value("access-token"))
                .andExpect(jsonPath("$.token_type").value("Bearer"))
                .andExpect(header().string("Set-Cookie", Matchers.containsString("refresh_token=refresh-token")))
                .andExpect(header().string("Set-Cookie", Matchers.containsString("HttpOnly")))
                .andExpect(header().string("Set-Cookie", Matchers.containsString("Secure")))
                .andExpect(header().string("Set-Cookie", Matchers.containsString("SameSite=Strict")))
                .andExpect(header().string("Set-Cookie", Matchers.containsString("Path=/api/v1/auth/reissue")));
    }

    @Test
    void 구글_콜백_동일_이메일_중복_실패() throws Exception {
        // given
        given(authService.handleGoogleCallback("dup-code"))
                .willThrow(new DuplicateOAuthEmailException("dup@gmail.com"));

        // when & then
        mockMvc.perform(get("/auth/oauth2/callback/google")
                        .param("code", "dup-code"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("DUPLICATE_USER"));
    }

    @Test
    void 구글_콜백_인가코드_오류_실패() throws Exception {
        // given
        given(authService.handleGoogleCallback("bad-code"))
                .willThrow(new GoogleTokenExchangeFailedException("Google token 교환에 실패했습니다."));

        // when & then
        mockMvc.perform(get("/auth/oauth2/callback/google")
                        .param("code", "bad-code"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("INVALID_TOKEN"));
    }

    @Test
    void 구글_콜백_사용자정보_조회_실패() throws Exception {
        // given
        given(authService.handleGoogleCallback("bad-userinfo"))
                .willThrow(new GoogleUserInfoFetchFailedException("Google 사용자 정보 조회에 실패했습니다."));

        // when & then
        mockMvc.perform(get("/auth/oauth2/callback/google")
                        .param("code", "bad-userinfo"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("SERVER_ERROR"));
    }

    @Test
    void 깃허브_연동_시작_리다이렉트_성공() throws Exception {
        // given
        URI redirectUri = URI.create(
                "https://github.com/login/oauth/authorize?client_id=test-github-client-id"
                        + "&redirect_uri=http://localhost:5173/github/redirect"
                        + "&response_type=code&scope=read:user%20repo&state=onboarding-token"
        );
        given(authService.buildGithubAuthorizationRedirect("Bearer onboarding-token"))
                .willReturn(redirectUri);

        // when & then
        mockMvc.perform(get("/auth/oauth2/github")
                        .header("Authorization", "Bearer onboarding-token"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", Matchers.containsString("client_id=test-github-client-id")))
                .andExpect(header().string("Location", Matchers.containsString("state=onboarding-token")));
    }

    @Test
    void 깃허브_연동_시작_Authorization_헤더_누락_실패() throws Exception {
        // given
        given(authService.buildGithubAuthorizationRedirect(null))
                .willThrow(new InvalidOnboardingTokenException("Authorization 헤더가 없습니다."));

        // when & then
        mockMvc.perform(get("/auth/oauth2/github"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("INVALID_TOKEN"));
    }

    @Test
    void 깃허브_연동_시작_Bearer_형식_오류_실패() throws Exception {
        // given
        given(authService.buildGithubAuthorizationRedirect("Token onboarding-token"))
                .willThrow(new InvalidOnboardingTokenException("Authorization 헤더는 Bearer 형식이어야 합니다."));

        // when & then
        mockMvc.perform(get("/auth/oauth2/github")
                        .header("Authorization", "Token onboarding-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("INVALID_TOKEN"));
    }

    @Test
    void 깃허브_콜백_성공() throws Exception {
        // given
        GithubOAuthCallbackResponse response = GithubOAuthCallbackResponse.of("service-access-token", 11L);
        given(authService.handleGithubCallback("valid-code", "onboarding-token"))
                .willReturn(GithubAuthTokenBundle.builder()
                        .response(response)
                        .refreshToken("service-refresh-token")
                        .build());

        // when & then
        mockMvc.perform(get("/auth/oauth2/callback/github")
                        .param("code", "valid-code")
                        .param("state", "onboarding-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("service-access-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.userId").value(11L))
                .andExpect(header().string("Set-Cookie", Matchers.containsString("refresh_token=service-refresh-token")))
                .andExpect(header().string("Set-Cookie", Matchers.containsString("Path=/api/v1/auth/reissue")));
    }

    @Test
    void 깃허브_콜백_state_누락_실패() throws Exception {
        // given
        given(authService.handleGithubCallback("valid-code", null))
                .willThrow(new InvalidOnboardingTokenException("Authorization 헤더가 없습니다."));

        // when & then
        mockMvc.perform(get("/auth/oauth2/callback/github")
                        .param("code", "valid-code"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("INVALID_TOKEN"));
    }

    @Test
    void 깃허브_콜백_code_누락_실패() throws Exception {
        // given
        given(authService.handleGithubCallback(null, "onboarding-token"))
                .willThrow(new GithubAuthorizationCodeMissingException());

        // when & then
        mockMvc.perform(get("/auth/oauth2/callback/github")
                        .param("state", "onboarding-token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_INPUT"));
    }

    @Test
    void 깃허브_콜백_토큰_교환_실패() throws Exception {
        // given
        given(authService.handleGithubCallback("bad-code", "onboarding-token"))
                .willThrow(new GithubTokenExchangeFailedException("GitHub token 교환에 실패했습니다."));

        // when & then
        mockMvc.perform(get("/auth/oauth2/callback/github")
                        .param("code", "bad-code")
                        .param("state", "onboarding-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("INVALID_TOKEN"));
    }

    @Test
    void 깃허브_콜백_사용자정보_조회_실패() throws Exception {
        // given
        given(authService.handleGithubCallback("valid-code", "onboarding-token"))
                .willThrow(new GithubUserInfoFetchFailedException("GitHub 사용자 정보 조회에 실패했습니다."));

        // when & then
        mockMvc.perform(get("/auth/oauth2/callback/github")
                        .param("code", "valid-code")
                        .param("state", "onboarding-token"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("SERVER_ERROR"));
    }

    @Test
    void 깃허브_콜백_중복_계정_실패() throws Exception {
        // given
        given(authService.handleGithubCallback("valid-code", "onboarding-token"))
                .willThrow(new DuplicateGithubAccountException("github-login"));

        // when & then
        mockMvc.perform(get("/auth/oauth2/callback/github")
                        .param("code", "valid-code")
                        .param("state", "onboarding-token"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("DUPLICATE_USER"));
    }

    @Test
    void 토큰_재발급_성공() throws Exception {
        // given
        AuthReissueTokenBundle tokenBundle = AuthReissueTokenBundle.builder()
                .response(AuthReissueResponse.of("new-access-token"))
                .refreshToken("new-refresh-token")
                .build();
        given(authService.reissueAccessToken("valid-refresh-token")).willReturn(tokenBundle);

        // when & then
        mockMvc.perform(post("/auth/reissue")
                        .cookie(new Cookie("refresh_token", "valid-refresh-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(header().string("Set-Cookie", Matchers.containsString("refresh_token=new-refresh-token")))
                .andExpect(header().string("Set-Cookie", Matchers.containsString("HttpOnly")))
                .andExpect(header().string("Set-Cookie", Matchers.containsString("Secure")))
                .andExpect(header().string("Set-Cookie", Matchers.containsString("SameSite=Strict")))
                .andExpect(header().string("Set-Cookie", Matchers.containsString("Path=/api/v1/auth/reissue")));
    }

    @Test
    void 토큰_재발급_refresh_token_cookie_누락_실패() throws Exception {
        // given
        given(authService.reissueAccessToken(null))
                .willThrow(new InvalidRefreshTokenException("refresh token cookie가 없습니다."));

        // when & then
        mockMvc.perform(post("/auth/reissue"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("INVALID_TOKEN"));
    }

    @Test
    void 토큰_재발급_RTR_위반_실패() throws Exception {
        // given
        given(authService.reissueAccessToken("reused-refresh-token"))
                .willThrow(new AlreadyUsedRefreshTokenException("이미 사용된 refresh token 입니다."));

        // when & then
        mockMvc.perform(post("/auth/reissue")
                        .cookie(new Cookie("refresh_token", "reused-refresh-token")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("ALREADY_USED_TOKEN"));
    }
}
