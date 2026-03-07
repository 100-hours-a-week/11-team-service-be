package com.thunder11.scuad.auth.controller;

import com.thunder11.scuad.auth.util.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * [로컬 개발 전용] 카카오 OAuth 없이 JWT 발급하는 컨트롤러
 *
 * 목적: 로컬 환경에서 k6 성능 테스트 시 토큰을 직접 발급받기 위함
 * 안전성: @Profile("local") 으로 인해 로컬 환경에서만 빈 등록 → 운영/개발 서버에는 절대 노출 안 됨
 * 사용법: GET /dev/token?userId=1&role=USER
 */
@Profile("local")
@RestController
@RequestMapping("/dev")
@RequiredArgsConstructor
public class DevAuthController {

    private final JwtProvider jwtProvider;

    /**
     * 로컬 테스트용 AccessToken 발급
     *
     * @param userId DB에 존재하는 user_id (시드 데이터 기준 1 또는 2)
     * @param role   USER 또는 ADMIN (기본값: USER)
     * @return accessToken 문자열
     */
    @GetMapping("/token")
    public Map<String, String> issueDevToken(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "USER") String role
    ) {
        String accessToken = jwtProvider.generateAccessToken(userId, role);
        return Map.of(
                "accessToken", accessToken,
                "userId", userId.toString(),
                "role", role,
                "usage", "Authorization: Bearer " + accessToken
        );
    }
}
