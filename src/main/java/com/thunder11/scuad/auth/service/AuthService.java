package com.thunder11.scuad.auth.service;

import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import com.thunder11.scuad.auth.config.KakaoProperties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// 인증/인가 비즈니스 로직 처리
// 카카오 OAuth 로그인, JWT 발급, 토큰 재발급 등
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final KakaoProperties kakaoProperties;

    // 카카오 OAuth 인증 URL 생성
    // 프론트엔드를 카카오 로그인 페이지로 리다이렉트하기 위한 URL 생성
    public String getKakaoAuthUrl(String state) {
        // UriComponentsBuilder로 쿼리 파라미터 안전하게 조합
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString("https://kauth.kakao.com/oauth/authorize")
                .queryParam("client_id", kakaoProperties.getClientId())
                .queryParam("redirect_uri", kakaoProperties.getRedirectUri())
                .queryParam("response_type", "code");

        // state가 있으면 추가 (CSRF 방지용)
        if (state != null && !state.isEmpty()) {
            builder.queryParam("state", state);
        }

        String authUrl = builder.toUriString();
        log.info("카카오 OAuth 인증 URL 생성: redirectUri={}", kakaoProperties.getRedirectUri());

        return authUrl;
    }
}