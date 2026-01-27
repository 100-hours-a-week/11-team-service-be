package com.thunder11.scuad.auth.dto;

import lombok.Builder;
import lombok.Getter;

// JWT 로그인 응답 DTO
// 카카오 로그인 성공 시 프론트엔드로 전달할 토큰 정보
@Getter
@Builder
public class LoginResponse {

    // JWT 액세스 토큰 (Authorization 헤더에 사용)
    private String accessToken;

    // JWT 리프레시 토큰 (토큰 재발급에 사용)
    private String refreshToken;

    // 토큰 타입 (Bearer)
    private String tokenType;

    // 액세스 토큰 만료 시간 (초 단위)
    private Long expiresIn;

    public static LoginResponse of(String accessToken, String refreshToken, Long expiresIn) {
        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(expiresIn)
                .build();
    }
}
