package com.thunder11.scuad.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 카카오 토큰 발급 API 응답 DTO
// POST https://kauth.kakao.com/oauth/token 의 응답을 매핑
@Getter
@NoArgsConstructor
public class KakaoTokenResponse {

    // 카카오 액세스 토큰
    @JsonProperty("access_token")
    private String accessToken;

    // 토큰 타입
    @JsonProperty("token_type")
    private String tokenType;

    // 카카오 리프레시 토큰
    @JsonProperty("refresh_token")
    private String refreshToken;

    // 액세스 토큰 만료 시간
    @JsonProperty("expires_in")
    private Integer expiresIn;

    // 리프레시 토큰 만료 시간
    @JsonProperty("refresh_token_expires_in")
    private Integer refreshTokenExpiresIn;
}