package com.thunder11.scuad.auth.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

import com.thunder11.scuad.auth.service.AuthService;

import lombok.RequiredArgsConstructor;

// 인증/인가 관련 API 컨트롤러
// 카카오 OAuth 로그인, 토큰 재발급, 로그아웃 등 처리
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
}