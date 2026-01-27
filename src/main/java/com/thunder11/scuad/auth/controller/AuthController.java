package com.thunder11.scuad.auth.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.util.UriComponentsBuilder;

import com.thunder11.scuad.auth.dto.LoginResponse;
import com.thunder11.scuad.auth.service.AuthService;
import com.thunder11.scuad.auth.dto.RefreshTokenRequest;
import com.thunder11.scuad.auth.dto.TokenRefreshResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// 인증/인가 관련 API 컨트롤러
// 카카오 OAuth 로그인, 토큰 재발급, 로그아웃 등 처리
@Slf4j
@RestController
@RequestMapping("/api/v1/auth/kakao")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // 카카오 로그인 시작 (카카오 인증 페이지로 리다이렉트)
    // 프론트엔드가 이 엔드포인트를 호출하면 카카오 OAuth 페이지로 이동
    @GetMapping("/login")
    public RedirectView startKakaoLogin(
            @RequestParam(required = false) String state  // CSRF 방지용 (선택)
    ) {
        // 카카오 OAuth 인증 URL 생성
        String kakaoAuthUrl = authService.getKakaoAuthUrl(state);

        // 302 리다이렉트
        return new RedirectView(kakaoAuthUrl);
    }

    // 카카오 로그인 콜백 처리
    // 카카오가 인증 완료 후 이 엔드포인트로 리다이렉트
    // 인가 코드를 받아서 JWT 토큰 발급
    @GetMapping("/callback")
    public RedirectView handleKakaoCallback(
            @RequestParam String code,  // 카카오 인가 코드
            @RequestParam(required = false) String state  // CSRF 방지용 (선택)
    ) {
        log.info("카카오 콜백 처리 시작: code={}", code.substring(0, 10) + "...");

        // 인가 코드로 로그인 처리 및 JWT 발급
        LoginResponse loginResponse = authService.processKakaoCallback(code);

        // 프론트엔드로 리다이렉트 (JWT 토큰 전달)
        // TODO: 프론트엔드 URL은 환경변수로 관리 필요
        String frontendUrl = UriComponentsBuilder
                .fromUriString("http://localhost:3000/auth/callback")  // 프론트엔드 콜백 URL
                .queryParam("accessToken", loginResponse.getAccessToken())
                .queryParam("refreshToken", loginResponse.getRefreshToken())
                .queryParam("expiresIn", loginResponse.getExpiresIn())
                .build()
                .toUriString();

        log.info("로그인 성공, 프론트엔드로 리다이렉트");
        return new RedirectView(frontendUrl);
    }

    // Access Token 재발급
    // Refresh Token을 받아서 새로운 Access Token과 Refresh Token 발급
    @PostMapping("/refresh")
    public TokenRefreshResponse refreshToken(
            @Valid @RequestBody RefreshTokenRequest request
    ) {
        log.info("토큰 재발급 요청: refreshToken={}", request.getRefreshToken().substring(0, 10) + "...");

        // Refresh Token으로 새 토큰 발급
        TokenRefreshResponse response = authService.refreshAccessToken(request.getRefreshToken());

        log.info("토큰 재발급 완료");
        return response;
    }
}