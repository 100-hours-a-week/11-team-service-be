package com.thunder11.scuad.common.controller;

import java.util.Map;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.thunder11.scuad.auth.util.JwtProvider;

/**
 * [로컬 개발 전용] 테스트용 JWT 토큰 발급 컨트롤러
 *
 * - local 프로필에서만 활성화 (운영/개발 서버에는 절대 노출 안 됨)
 * - 카카오 OAuth 없이 userId 기반으로 토큰 즉시 발급
 * - SecurityConfig에서 /api/test/** 는 이미 permitAll() 처리됨
 *
 * 사용 예시:
 *   curl "http://localhost:8080/api/test/token?userId=1&role=USER"
 */
@Slf4j
@Profile("local")
@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class DevTokenController {

    private final JwtProvider jwtProvider;

    @GetMapping("/token")
    public Map<String, String> issueTestToken(
            @RequestParam(defaultValue = "1") Long userId,
            @RequestParam(defaultValue = "USER") String role
    ) {
        log.warn("[DevTokenController] 테스트용 토큰 발급 - userId={}, role={}", userId, role);

        String accessToken = jwtProvider.generateAccessToken(userId, role);

        return Map.of(
                "accessToken", accessToken,
                "userId", String.valueOf(userId),
                "role", role,
                "usage", "Authorization: Bearer " + accessToken
        );
    }
}
