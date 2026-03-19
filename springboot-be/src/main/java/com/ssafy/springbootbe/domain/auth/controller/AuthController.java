package com.ssafy.springbootbe.domain.auth.controller;

import com.ssafy.springbootbe.domain.auth.dto.response.AuthTokenBundle;
import com.ssafy.springbootbe.domain.auth.dto.response.AuthReissueResponse;
import com.ssafy.springbootbe.domain.auth.dto.response.AuthReissueTokenBundle;
import com.ssafy.springbootbe.domain.auth.dto.response.GithubAuthTokenBundle;
import com.ssafy.springbootbe.domain.auth.dto.response.GithubOAuthCallbackResponse;
import com.ssafy.springbootbe.domain.auth.dto.response.GoogleOAuthCallbackResponse;
import com.ssafy.springbootbe.domain.auth.exception.AuthCookieProcessingException;
import com.ssafy.springbootbe.domain.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String REFRESH_COOKIE_NAME = "refresh_token";
    private static final String REFRESH_COOKIE_PATH = "/api/v1/auth/reissue";

    private final AuthService authService;

    @GetMapping("/login/google")
    public ResponseEntity<GoogleOAuthCallbackResponse> loginWithGoogle(
            @RequestParam(required = false) String code) {
        AuthTokenBundle tokenBundle = authService.loginWithGoogle(code);
        ResponseEntity.BodyBuilder responseBuilder = ResponseEntity.ok();

        if (tokenBundle.hasRefreshToken()) {
            responseBuilder.header(HttpHeaders.SET_COOKIE, createRefreshTokenCookie(tokenBundle.getRefreshToken()).toString());
        }

        return responseBuilder.body(tokenBundle.getResponse());
    }

    @GetMapping("/link-github")
    public ResponseEntity<GithubOAuthCallbackResponse> linkGithub(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state) {
        GithubAuthTokenBundle tokenBundle = authService.linkGithub(code, state);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, createRefreshTokenCookie(tokenBundle.getRefreshToken()).toString())
                .body(tokenBundle.getResponse());
    }

    @PostMapping("/reissue")
    public ResponseEntity<AuthReissueResponse> reissueAccessToken(
            @CookieValue(value = REFRESH_COOKIE_NAME, required = false) String refreshToken) {
        AuthReissueTokenBundle tokenBundle = authService.reissueAccessToken(refreshToken);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, createRefreshTokenCookie(tokenBundle.getRefreshToken()).toString())
                .body(tokenBundle.getResponse());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader) {
        authService.logout(authorizationHeader);

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, createExpiredRefreshTokenCookie().toString())
                .build();
    }

    private ResponseCookie createRefreshTokenCookie(String refreshToken) {
        try {
            return ResponseCookie.from(REFRESH_COOKIE_NAME, refreshToken)
                    .httpOnly(true)
                    .secure(true)
                    .sameSite("Strict")
                    .path(REFRESH_COOKIE_PATH)
                    .maxAge(Duration.ofDays(7))
                    .build();
        } catch (RuntimeException e) {
            throw new AuthCookieProcessingException("refresh token cookie 생성에 실패했습니다.", e);
        }
    }

    private ResponseCookie createExpiredRefreshTokenCookie() {
        try {
            return ResponseCookie.from(REFRESH_COOKIE_NAME, "")
                    .httpOnly(true)
                    .secure(true)
                    .sameSite("Strict")
                    .path(REFRESH_COOKIE_PATH)
                    .maxAge(Duration.ZERO)
                    .build();
        } catch (RuntimeException e) {
            throw new AuthCookieProcessingException("refresh token cookie 만료 처리에 실패했습니다.", e);
        }
    }
}
