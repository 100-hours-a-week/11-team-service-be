package com.thunder11.scuad.auth.controller;

import java.util.Map;

import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.thunder11.scuad.auth.util.JwtProvider;

// [로컬 전용] k6 성능 테스트용 JWT 토큰 발급 컨트롤러
// 카카오 OAuth 없이 userId 기반으로 토큰을 발급하여 부하 테스트 진행
// @Profile("local"): 운영/개발 서버에는 절대 노출되지 않음
@Slf4j
@RestController
@RequestMapping("/dev")
@RequiredArgsConstructor
@Profile("local")
public class DevAuthController {

    private final JwtProvider jwtProvider;

    // 테스트용 토큰 발급
    // 사용법: curl "http://localhost:8080/dev/token?userId=1&role=USER"
    @GetMapping("/token")
    public ResponseEntity<Map<String, String>> issueTestToken(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "USER") String role
    ) {
        log.warn("[DEV ONLY] 테스트 토큰 발급: userId={}, role={}", userId, role);

        String accessToken = jwtProvider.generateAccessToken(userId, role);

        return ResponseEntity.ok(Map.of(
                "accessToken", accessToken,
                "userId", userId.toString(),
                "role", role,
                "usage", "Authorization: Bearer " + accessToken
        ));
    }
}
