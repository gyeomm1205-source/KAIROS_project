package com.ssafy.springbootbe.domain.auth.controller;

import com.ssafy.springbootbe.domain.auth.dto.response.AuthTokenBundle;
import com.ssafy.springbootbe.domain.auth.dto.response.GoogleOAuthCallbackResponse;
import com.ssafy.springbootbe.domain.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
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

    private ResponseCookie createRefreshTokenCookie(String refreshToken) {
        return ResponseCookie.from(REFRESH_COOKIE_NAME, refreshToken)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path(REFRESH_COOKIE_PATH)
                .maxAge(Duration.ofDays(7))
                .build();
    }
}
