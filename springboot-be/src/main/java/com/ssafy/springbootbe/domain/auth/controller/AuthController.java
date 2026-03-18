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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.net.URI;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String REFRESH_COOKIE_NAME = "refresh_token";
    private static final String REFRESH_COOKIE_PATH = "/api/v1/auth/reissue";

    private final AuthService authService;

    @Value("${oauth.google.auth_uri}")
    private String googleAuthUri;

    @Value("${oauth.google.client_id}")
    private String googleClientId;

    @Value("${oauth.google.redirect_uri}")
    private String googleRedirectUri;

    @Value("${oauth.google.scope}")
    private String googleScope;

    @GetMapping("/oauth2/google")
    public ResponseEntity<Void> redirectToGoogleLogin() {
        URI redirectUri = UriComponentsBuilder.fromUriString(googleAuthUri)
                .queryParam("client_id", googleClientId)
                .queryParam("redirect_uri", googleRedirectUri)
                .queryParam("response_type", "code")
                .queryParam("scope", googleScope)
                .build()
                .encode()
                .toUri();

        return ResponseEntity.status(302)
                .location(redirectUri)
                .build();
    }

    @GetMapping("/oauth2/callback/google")
    public ResponseEntity<GoogleOAuthCallbackResponse> handleGoogleCallback(
            @RequestParam(required = false) String code) {
        AuthTokenBundle tokenBundle = authService.handleGoogleCallback(code);
        ResponseEntity.BodyBuilder responseBuilder = ResponseEntity.ok();

        if (tokenBundle.hasRefreshToken()) {
            responseBuilder.header(HttpHeaders.SET_COOKIE, createRefreshTokenCookie(tokenBundle.getRefreshToken()).toString());
        }

        return responseBuilder.body(tokenBundle.getResponse());
    }

    @GetMapping("/oauth2/github")
    public ResponseEntity<Void> redirectToGithubLogin(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader) {
        URI redirectUri = authService.buildGithubAuthorizationRedirect(authorizationHeader);
        return ResponseEntity.status(302)
                .location(redirectUri)
                .build();
    }

    @GetMapping("/oauth2/callback/github")
    public ResponseEntity<GithubOAuthCallbackResponse> handleGithubCallback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state) {
        GithubAuthTokenBundle tokenBundle = authService.handleGithubCallback(code, state);

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
