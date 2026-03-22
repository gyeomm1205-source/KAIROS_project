package com.ssafy.springbootbe.domain.auth.controller;

import com.ssafy.springbootbe.common.jwt.JWTUtils;
import com.ssafy.springbootbe.common.redis.RedisService;
import com.ssafy.springbootbe.domain.auth.dto.request.LinkGithubRequest;
import com.ssafy.springbootbe.domain.auth.dto.request.LinkVelogRequest;
import com.ssafy.springbootbe.domain.auth.dto.response.AuthReissueResponse;
import com.ssafy.springbootbe.domain.auth.dto.response.AuthReissueTokenBundle;
import com.ssafy.springbootbe.domain.auth.dto.response.AuthTokenBundle;
import com.ssafy.springbootbe.domain.auth.dto.response.GithubAuthTokenBundle;
import com.ssafy.springbootbe.domain.auth.dto.response.GithubOAuthCallbackResponse;
import com.ssafy.springbootbe.domain.auth.dto.response.LinkVelogResponse;
import com.ssafy.springbootbe.domain.auth.dto.response.GoogleOAuthCallbackResponse;
import com.ssafy.springbootbe.domain.auth.exception.AlreadyUsedRefreshTokenException;
import com.ssafy.springbootbe.domain.auth.exception.DuplicateGithubAccountException;
import com.ssafy.springbootbe.domain.auth.exception.GithubAuthorizationCodeMissingException;
import com.ssafy.springbootbe.domain.auth.exception.GithubTokenExchangeFailedException;
import com.ssafy.springbootbe.domain.auth.exception.DuplicateOAuthEmailException;
import com.ssafy.springbootbe.domain.auth.exception.GoogleTokenExchangeFailedException;
import com.ssafy.springbootbe.domain.auth.exception.GoogleUserInfoFetchFailedException;
import com.ssafy.springbootbe.domain.auth.exception.GithubUserInfoFetchFailedException;
import com.ssafy.springbootbe.domain.auth.exception.InvalidAccessTokenException;
import com.ssafy.springbootbe.domain.auth.exception.InvalidRefreshTokenException;
import com.ssafy.springbootbe.domain.auth.exception.InvalidOnboardingTokenException;
import com.ssafy.springbootbe.domain.auth.exception.VelogCollectAsyncFailedException;
import com.ssafy.springbootbe.domain.auth.service.AuthService;
import io.jsonwebtoken.Claims;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.servlet.http.Cookie;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JWTUtils jwtUtils;

    @MockitoBean
    private RedisService redisService;

    @BeforeEach
    void setUp() {
        Claims claims = mock(Claims.class);
        given(claims.get("userId")).willReturn(1L);
        given(claims.get("userId", Long.class)).willReturn(1L);
        given(claims.get("nickName", String.class)).willReturn("tester");
        given(claims.get("email", String.class)).willReturn("tester@example.com");
        given(jwtUtils.getClaims(anyString())).willReturn(claims);
    }

    @Test
    void 구글_콜백_신규_유저_성공() throws Exception {
        // given
        GoogleOAuthCallbackResponse response = GoogleOAuthCallbackResponse.forNewUser(
                "onboarding-token",
                "new-user@gmail.com",
                "https://image.example/profile.png"
        );
        given(authService.loginWithGoogle("valid-code"))
                .willReturn(AuthTokenBundle.newUser(response));

        // when & then
        mockMvc.perform(post("/auth/login/google")
                        .contentType("application/json")
                        .content("""
                                {
                                  "code": "valid-code"
                                }
                                """))
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
        given(authService.loginWithGoogle("valid-code"))
                .willReturn(AuthTokenBundle.existingUser(response, "refresh-token"));

        // when & then
        mockMvc.perform(post("/auth/login/google")
                        .contentType("application/json")
                        .content("""
                                {
                                  "code": "valid-code"
                                }
                                """))
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
        given(authService.loginWithGoogle("dup-code"))
                .willThrow(new DuplicateOAuthEmailException("dup@gmail.com"));

        // when & then
        mockMvc.perform(post("/auth/login/google")
                        .contentType("application/json")
                        .content("""
                                {
                                  "code": "dup-code"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("DUPLICATE_USER"));
    }

    @Test
    void 구글_콜백_code_누락_실패() throws Exception {
        // when & then
        mockMvc.perform(post("/auth/login/google")
                        .contentType("application/json")
                        .content("""
                                {
                                  "code": " "
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_INPUT"));
    }

    @Test
    void 구글_콜백_인가코드_오류_실패() throws Exception {
        // given
        given(authService.loginWithGoogle("bad-code"))
                .willThrow(new GoogleTokenExchangeFailedException("Google token 교환에 실패했습니다."));

        // when & then
        mockMvc.perform(post("/auth/login/google")
                        .contentType("application/json")
                        .content("""
                                {
                                  "code": "bad-code"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("INVALID_TOKEN"));
    }

    @Test
    void 구글_콜백_사용자정보_조회_실패() throws Exception {
        // given
        given(authService.loginWithGoogle("bad-userinfo"))
                .willThrow(new GoogleUserInfoFetchFailedException("Google 사용자 정보 조회에 실패했습니다."));

        // when & then
        mockMvc.perform(post("/auth/login/google")
                        .contentType("application/json")
                        .content("""
                                {
                                  "code": "bad-userinfo"
                                }
                                """))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("SERVER_ERROR"));
    }

    @Test
    void 깃허브_콜백_성공() throws Exception {
        // given
        GithubOAuthCallbackResponse response = GithubOAuthCallbackResponse.of("service-access-token", 11L, "task_github_abc");
        given(authService.linkGithub(org.mockito.ArgumentMatchers.eq("Bearer onboarding-token"),
                org.mockito.ArgumentMatchers.any(LinkGithubRequest.class)))
                .willReturn(GithubAuthTokenBundle.builder()
                        .response(response)
                        .refreshToken("service-refresh-token")
                        .build());

        // when & then
        mockMvc.perform(post("/auth/link-github")
                        .header("Authorization", "Bearer onboarding-token")
                        .contentType("application/json")
                        .content("""
                                {
                                  "code": "valid-code"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("service-access-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.userId").value(11L))
                .andExpect(jsonPath("$.githubTaskId").value("task_github_abc"))
                .andExpect(header().string("Set-Cookie", Matchers.containsString("refresh_token=service-refresh-token")))
                .andExpect(header().string("Set-Cookie", Matchers.containsString("Path=/api/v1/auth/reissue")));
    }

    @Test
    void 깃허브_콜백_Authorization_헤더_누락_실패() throws Exception {
        // given
        given(authService.linkGithub(org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.any(LinkGithubRequest.class)))
                .willThrow(new InvalidOnboardingTokenException("Authorization 헤더가 없습니다."));

        // when & then
        mockMvc.perform(post("/auth/link-github")
                        .contentType("application/json")
                        .content("""
                                {
                                  "code": "valid-code"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("INVALID_TOKEN"));
    }

    @Test
    void 깃허브_콜백_code_누락_실패() throws Exception {
        // when & then
        mockMvc.perform(post("/auth/link-github")
                        .header("Authorization", "Bearer onboarding-token")
                        .contentType("application/json")
                        .content("""
                                {
                                  "code": " "
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_INPUT"));
    }

    @Test
    void 깃허브_콜백_토큰_교환_실패() throws Exception {
        // given
        given(authService.linkGithub(org.mockito.ArgumentMatchers.eq("Bearer onboarding-token"),
                org.mockito.ArgumentMatchers.any(LinkGithubRequest.class)))
                .willThrow(new GithubTokenExchangeFailedException("GitHub token 교환에 실패했습니다."));

        // when & then
        mockMvc.perform(post("/auth/link-github")
                        .header("Authorization", "Bearer onboarding-token")
                        .contentType("application/json")
                        .content("""
                                {
                                  "code": "bad-code"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("INVALID_TOKEN"));
    }

    @Test
    void 깃허브_콜백_사용자정보_조회_실패() throws Exception {
        // given
        given(authService.linkGithub(org.mockito.ArgumentMatchers.eq("Bearer onboarding-token"),
                org.mockito.ArgumentMatchers.any(LinkGithubRequest.class)))
                .willThrow(new GithubUserInfoFetchFailedException("GitHub 사용자 정보 조회에 실패했습니다."));

        // when & then
        mockMvc.perform(post("/auth/link-github")
                        .header("Authorization", "Bearer onboarding-token")
                        .contentType("application/json")
                        .content("""
                                {
                                  "code": "valid-code"
                                }
                                """))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("SERVER_ERROR"));
    }

    @Test
    void 깃허브_콜백_중복_계정_실패() throws Exception {
        // given
        given(authService.linkGithub(org.mockito.ArgumentMatchers.eq("Bearer onboarding-token"),
                org.mockito.ArgumentMatchers.any(LinkGithubRequest.class)))
                .willThrow(new DuplicateGithubAccountException("github-login"));

        // when & then
        mockMvc.perform(post("/auth/link-github")
                        .header("Authorization", "Bearer onboarding-token")
                        .contentType("application/json")
                        .content("""
                                {
                                  "code": "valid-code"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("DUPLICATE_USER"));
    }

    @Test
    void 벨로그_연동_성공() throws Exception {
        // given
        given(authService.linkVelog(org.mockito.ArgumentMatchers.eq("Bearer valid-access-token"),
                org.mockito.ArgumentMatchers.any(LinkVelogRequest.class)))
                .willReturn(LinkVelogResponse.of("teddynu", "task_velog_xyz"));

        // when & then
        mockMvc.perform(post("/auth/link-velog")
                        .header("Authorization", "Bearer valid-access-token")
                        .contentType("application/json")
                        .content("""
                                {
                                  "velogUsername": "teddynu"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.velogUsername").value("teddynu"))
                .andExpect(jsonPath("$.velogTaskId").value("task_velog_xyz"));
    }

    @Test
    void 벨로그_연동_velogUsername_누락_실패() throws Exception {
        // when & then
        mockMvc.perform(post("/auth/link-velog")
                        .header("Authorization", "Bearer valid-access-token")
                        .contentType("application/json")
                        .content("""
                                {
                                  "velogUsername": " "
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_INPUT"));
    }

    @Test
    void 벨로그_연동_인증_실패() throws Exception {
        // given
        given(authService.linkVelog(org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.any(LinkVelogRequest.class)))
                .willThrow(new InvalidAccessTokenException("Authorization 헤더가 없습니다."));

        // when & then
        mockMvc.perform(post("/auth/link-velog")
                        .contentType("application/json")
                        .content("""
                                {
                                  "velogUsername": "teddynu"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("INVALID_TOKEN"));
    }

    @Test
    void 벨로그_연동_FastAPI_실패() throws Exception {
        // given
        given(authService.linkVelog(org.mockito.ArgumentMatchers.eq("Bearer valid-access-token"),
                org.mockito.ArgumentMatchers.any(LinkVelogRequest.class)))
                .willThrow(new VelogCollectAsyncFailedException("FastAPI Velog 수집 호출에 실패했습니다."));

        // when & then
        mockMvc.perform(post("/auth/link-velog")
                        .header("Authorization", "Bearer valid-access-token")
                        .contentType("application/json")
                        .content("""
                                {
                                  "velogUsername": "teddynu"
                                }
                                """))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error").value("BAD_GATEWAY"));
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

    @Test
    void 로그아웃_성공() throws Exception {
        // given
        willDoNothing().given(authService).logout("Bearer valid-access-token");

        // when & then
        mockMvc.perform(post("/auth/logout")
                        .header("Authorization", "Bearer valid-access-token"))
                .andExpect(status().isNoContent())
                .andExpect(header().string("Set-Cookie", Matchers.containsString("refresh_token=")))
                .andExpect(header().string("Set-Cookie", Matchers.containsString("Max-Age=0")))
                .andExpect(header().string("Set-Cookie", Matchers.containsString("HttpOnly")))
                .andExpect(header().string("Set-Cookie", Matchers.containsString("Secure")))
                .andExpect(header().string("Set-Cookie", Matchers.containsString("SameSite=Strict")))
                .andExpect(header().string("Set-Cookie", Matchers.containsString("Path=/api/v1/auth/reissue")));
    }

    @Test
    void 로그아웃_Authorization_헤더_누락_실패() throws Exception {
        // given
        willThrow(new InvalidAccessTokenException("Authorization 헤더가 없습니다."))
                .given(authService).logout(null);

        // when & then
        mockMvc.perform(post("/auth/logout"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("INVALID_TOKEN"));
    }

    @Test
    void 로그아웃_Bearer_형식_오류_실패() throws Exception {
        // given
        willThrow(new InvalidAccessTokenException("Authorization 헤더는 Bearer 형식이어야 합니다."))
                .given(authService).logout("Token invalid");

        // when & then
        mockMvc.perform(post("/auth/logout")
                        .header("Authorization", "Token invalid"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("INVALID_TOKEN"));
    }

    @Test
    void 로그아웃_유효하지_않은_access_token_실패() throws Exception {
        // given
        willThrow(new InvalidAccessTokenException("유효하지 않은 access token 입니다."))
                .given(authService).logout("Bearer expired-access-token");

        // when & then
        mockMvc.perform(post("/auth/logout")
                        .header("Authorization", "Bearer expired-access-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("INVALID_TOKEN"));
    }
}
