package com.ssafy.s14p21a506.auth.api;

import com.ssafy.s14p21a506.auth.AuthenticatedUser;
import com.ssafy.s14p21a506.auth.dto.CurrentUserResponse;
import com.ssafy.s14p21a506.config.security.CognitoProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/test")
public class AuthApi implements AuthApiDoc {

    private final CognitoProperties cognitoProperties;

    @Override
    @GetMapping("/me")
    public ResponseEntity<CurrentUserResponse> me(@AuthenticationPrincipal Jwt jwt) {
        AuthenticatedUser user = AuthenticatedUser.from(jwt, cognitoProperties.getUsernameClaim());
        return ResponseEntity.ok(CurrentUserResponse.from("springboot-be", user));
    }
}
