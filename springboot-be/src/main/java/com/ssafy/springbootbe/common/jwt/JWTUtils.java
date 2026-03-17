package com.ssafy.springbootbe.common.jwt;

import com.ssafy.springbootbe.persistence.user.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

@Component
public class JWTUtils {

    @Value("${service.access-token-duration}")
    private Long accessTokenDurationTime;

    @Value("${service.refresh-token-duration}")
    private Long refreshTokenDurationTime;

    private static final long ONBOARDING_TOKEN_DURATION_MINUTES = 30L;

    private SecretKey secretKey;
    public JWTUtils(@Value("${spring.jwt.secret}") String secret) {
        secretKey = new SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8),
                Jwts.SIG.HS256.key().build().getAlgorithm()
        );
    }
    public Claims getClaims(String token) {
        JwtParser parser = Jwts.parser().verifyWith(secretKey).build();
        var jws = parser.parseSignedClaims(token);
        return jws.getPayload();
    }
    public String createAccessToken(User user) {
        return createToken("accessToken", user, accessTokenDurationTime);
    }
    public String createRefreshToken(User user) {
        return createToken("refreshToken", user, refreshTokenDurationTime);
    }
    public String createOnboardingToken(String googleSub, String email) {
        Date expiration = new Date(System.currentTimeMillis() + 1000 * 60 * ONBOARDING_TOKEN_DURATION_MINUTES);
        return Jwts.builder()
                .subject("onboarding")
                .expiration(expiration)
                .claims(Map.of(
                        "googleSub", googleSub,
                        "email", email,
                        "purpose", "onboarding"
                ))
                .signWith(secretKey)
                .compact();
    }
    private String createToken(String subject, User user, long duration) {
        Date expiration = new Date(System.currentTimeMillis() + 1000*60*60*duration); // 시간(hour) 단위
        return Jwts.builder()
                .subject(subject)
                .expiration(expiration)
                .claims(parseToClaims(user))
                .signWith(secretKey)
                .compact();
    }
    private Map<String, Object> parseToClaims(User user) {
        return Map.of(
                "userId", user.getUserId(),
                "nickName", user.getNickname(),
                "email", user.getEmail(),
                "status", user.getStatus()
        );
    }
}
