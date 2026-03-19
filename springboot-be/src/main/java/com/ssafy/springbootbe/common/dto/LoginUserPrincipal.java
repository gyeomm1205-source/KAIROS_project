package com.ssafy.springbootbe.common.dto;

import io.jsonwebtoken.Claims;
import lombok.*;

@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginUserPrincipal {
    private Long userId;
    private String nickName;
    private String email;
    public static LoginUserPrincipal fromClaims(Claims claims) {
        Long userId = claims.get("userId", Long.class);
        String nickName = claims.get("nickName", String.class);
        String email = claims.get("email", String.class);

        return LoginUserPrincipal.builder()
                .userId(userId)
                .nickName(nickName)
                .email(email)
                .build();
    }
}
