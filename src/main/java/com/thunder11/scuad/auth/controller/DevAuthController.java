package com.thunder11.scuad.auth.controller;

import com.thunder11.scuad.auth.util.JwtProvider;
import com.thunder11.scuad.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * [로컬 전용] 부하 테스트용 JWT 토큰 발급 컨트롤러
 *
 * 의도: k6 setup() 함수에서 카카오 OAuth 없이 JWT를 직접 발급받아
 *      VU(Virtual User)에 전달하기 위해 사용합니다.
 *
 * 근거:
 * - SecurityConfig에 /api/test/** permitAll() 이미 설정됨 → SecurityConfig 수정 불필요
 * - @Profile("local") → 운영/개발 환경에서는 이 컨트롤러 빈이 생성되지 않음
 * - JwtProvider.generateAccessToken() 을 그대로 재사용 → 실제 인증과 동일한 토큰 구조
 *
 * 사용 예시:
 * GET http://localhost:8080/api/test/token?userId=1&role=USER
 */
@Profile("local")
@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class DevAuthController {

    private final JwtProvider jwtProvider;

    @GetMapping("/token")
    public ApiResponse<Map<String, String>> issueTestToken(
            @RequestParam(defaultValue = "1") Long userId,
            @RequestParam(defaultValue = "USER") String role
    ) {
        String accessToken = jwtProvider.generateAccessToken(userId, role);
        return ApiResponse.of(
                HttpStatus.OK.value(),
                "SUCCESS",
                "테스트 토큰 발급 완료",
                Map.of("accessToken", accessToken)
        );
    }
}
