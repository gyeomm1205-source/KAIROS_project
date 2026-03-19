package com.ssafy.springbootbe.domain.auth.dto.response;

import tools.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoogleOAuthCallbackResponse {

    @JsonProperty("is_new_user")
    private Boolean isNewUser;

    @JsonProperty("onboarding_token")
    private String onboardingToken;

    private String email;

    @JsonProperty("profile_image_url")
    private String profileImageUrl;

    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("token_type")
    private String tokenType;

    public static GoogleOAuthCallbackResponse forNewUser(
            String onboardingToken,
            String email,
            String profileImageUrl) {
        return GoogleOAuthCallbackResponse.builder()
                .isNewUser(true)
                .onboardingToken(onboardingToken)
                .email(email)
                .profileImageUrl(profileImageUrl)
                .build();
    }

    public static GoogleOAuthCallbackResponse forExistingUser(String accessToken) {
        return GoogleOAuthCallbackResponse.builder()
                .isNewUser(false)
                .accessToken(accessToken)
                .tokenType("Bearer")
                .build();
    }
}
