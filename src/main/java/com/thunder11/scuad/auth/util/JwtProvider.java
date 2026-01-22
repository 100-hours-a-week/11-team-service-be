package com.thunder11.scuad.auth.util;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import com.nimbusds.jwt.JWT;
import com.nimbusds.oauth2.sdk.auth.Secret;
import org.springframework.stereotype.Component;

import com.thunder11.scuad.auth.config.JwtProperties;
import com.thunder11.scuad.common.exception.ApiException;
import com.thunder11.scuad.common.exception.ErrorCode;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;


// JWT 토큰 생성 및 검증
// Access Token과 Refresh Token 생성, 검증, 파싱 기능 제공

@Component
@RequiredArgsConstructor
public class JwtProvider {

    private final JwtProperties jwtProperties;

    // Secret Key 생성
    // application.yml의 secret 문자열을 SecretKey 객체로 변환

    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // Access Token 생성, 사용자 ID와 Role을 포함한 JWT 토큰 생성
    public String generateAccessToken(Long userId, String role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtProperties.getAccessTokenExpiration());

        return Jwts.builder()
                .subject(userId.toString())  // sub: 사용자 ID
                .claim("role", role)         // role
                .issuedAt(now)               // iat: 발급 시각
                .expiration(expiryDate)      // exp: 만료 시각
                .signWith(getSigningKey())   // 서명
                .compact();
    }


    // Refresh Token 생성
    public String generateRefreshToken() {
        return UUID.randomUUID().toString();
    }

    // 토큰 유효성 검증
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            throw new ApiException(ErrorCode.ACCESS_TOKEN_EXPIRED);
        } catch (JwtException e) {
            throw new ApiException(ErrorCode.INVALID_TOKEN);
        }
    }

    // 토큰에서 사용자 ID 추출
    public Long getUserIdFromToken(String token) {
        Claims claims = parseClaims(token);
        return Long.parseLong(claims.getSubject());
    }

    // 토큰에서 Role 추출
    public String getRoleFromToken(String token) {
        Claims claims = parseClaims(token);
        return claims.get("role", String.class);
    }

    // 토큰 파싱하여 모든 데이터 추출
    private Claims parseClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            // 만료된 토큰도 Claims는 추출 가능 (재발급 등에서 사용)
            return e.getClaims();
        }
    }
}