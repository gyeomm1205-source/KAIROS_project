package com.ssafy.springbootbe.domain.auth.controller;

import com.ssafy.springbootbe.domain.auth.dto.response.AuthTokenBundle;
import com.ssafy.springbootbe.domain.auth.dto.response.GoogleOAuthCallbackResponse;
import com.ssafy.springbootbe.domain.auth.exception.DuplicateOAuthEmailException;
import com.ssafy.springbootbe.domain.auth.exception.GoogleTokenExchangeFailedException;
import com.ssafy.springbootbe.domain.auth.exception.GoogleUserInfoFetchFailedException;
import com.ssafy.springbootbe.domain.auth.service.AuthService;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        value = AuthController.class,
        properties = {
                "oauth.google.auth_uri=https://accounts.google.com/o/oauth2/auth",
                "oauth.google.client_id=test-google-client-id",
                "oauth.google.redirect_uri=http://localhost:5173/google/redirect",
                "oauth.google.scope=openid email profile"
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
}
