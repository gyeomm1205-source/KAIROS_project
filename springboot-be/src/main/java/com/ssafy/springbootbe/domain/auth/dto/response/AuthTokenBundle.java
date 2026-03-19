package com.ssafy.springbootbe.domain.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthTokenBundle {

    private GoogleOAuthCallbackResponse response;
    private String refreshToken;

    public static AuthTokenBundle newUser(GoogleOAuthCallbackResponse response) {
        return AuthTokenBundle.builder()
                .response(response)
                .build();
    }

    public static AuthTokenBundle existingUser(GoogleOAuthCallbackResponse response, String refreshToken) {
        return AuthTokenBundle.builder()
                .response(response)
                .refreshToken(refreshToken)
                .build();
    }

    public boolean hasRefreshToken() {
        return refreshToken != null && !refreshToken.isBlank();
    }
}
