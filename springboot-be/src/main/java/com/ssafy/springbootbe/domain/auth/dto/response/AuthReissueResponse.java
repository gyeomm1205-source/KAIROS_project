package com.ssafy.springbootbe.domain.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthReissueResponse {

    private String accessToken;
    private String tokenType;

    public static AuthReissueResponse of(String accessToken) {
        return AuthReissueResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .build();
    }
}
