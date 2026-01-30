package com.thunder11.scuad.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

// Refresh Token 재발급 요청 DTO
// 프론트엔드가 Refresh Token을 전달
@Getter
@NoArgsConstructor
public class RefreshTokenRequest {

    // Refresh Token (UUID 형식)
    @NotBlank(message = "Refresh Token은 필수입니다")
    private String refreshToken;
}