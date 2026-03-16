package com.ssafy.springbootbe.common.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;

@Component
public class JWTUtils {

    @Value("${service.access-token-duration}")
    private Long accessTokenDurationTime;

    @Value("${service.refresh-token-duration}")
    private Long refreshTokenDurationTime;

    private SecretKey secretKey;
    public JWTUtils() {
        secretKey = Jwts.SIG.HS256.key().build();
    }
    public Claims getClaims(String token) {
        JwtParser parser = Jwts.parser().verifyWith(secretKey).build();
        var jws = parser.parseSignedClaims(token);
        return jws.getPayload();
    }
//    public String createAccessToken(Member member) {
//        return createToken("accessToken", member, accessTokenDurationTime);
//    }
//    public String createRefreshToken(Member member) {
//        return createToken("refreshToken", member, refreshTokenDurationTime);
//    }
//    private String createToken(String subject, Member member, long duration) {
//        Date expiration = new Date(System.currentTimeMillis() + 1000*duration); // 분 단위
//        return Jwts.builder()
//                .subject(subject)
//                .expiration(expiration)
//                .claims(parseToClaims(member))
//                .signWith(secretKey)
//                .compact();
//    }
//    private Map<String, Object> parseToClaims(Member member) {
//        return Map.of(
//                "memberId", member.getMemberId(),
//                "name", member.getName(),
//                "email", member.getEmail()
//        );
//    }
}
