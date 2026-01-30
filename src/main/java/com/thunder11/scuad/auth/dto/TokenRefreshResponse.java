package com.thunder11.scuad.auth.dto;

import lombok.Builder;
import lombok.Getter;

// 토큰 재발급 응답 DTO
// 새로 발급된 Access Token과 Refresh Token 전달
@Getter
@Builder
public class TokenRefreshResponse {

    // 새로 발급된 Access Token
    private String accessToken;

    // 새로 발급된 Refresh Token (Refresh Token Rotation)
    private String refreshToken;

    // 토큰 타입 (Bearer)
    private String tokenType;

    // 액세스 토큰 만료 시간 (초 단위)
    private Long expiresIn;

    public static TokenRefreshResponse of(String accessToken, String refreshToken, Long expiresIn) {
        return TokenRefreshResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(expiresIn)
                .build();
    }
}