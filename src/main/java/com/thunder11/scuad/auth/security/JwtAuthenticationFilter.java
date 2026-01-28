package com.thunder11.scuad.auth.security;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.thunder11.scuad.auth.domain.Role;
import com.thunder11.scuad.auth.util.JwtProvider;
import com.thunder11.scuad.common.exception.ApiException;

// JWT 인증 필터
// Authorization 헤더에서 JWT를 추출하고 검증하여 SecurityContext에 인증 정보 저장
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            // 1. Authorization 헤더에서 JWT 추출
            String token = extractTokenFromRequest(request);

            // 2. 토큰이 있으면 검증 및 인증 처리
            if (token != null) {
                // 토큰 검증
                jwtProvider.validateToken(token);

                // 토큰에서 사용자 정보 추출
                Long userId = jwtProvider.getUserIdFromToken(token);
                String roleStr = jwtProvider.getRoleFromToken(token);
                Role role = Role.valueOf(roleStr);

                // UserPrincipal 생성
                UserPrincipal userPrincipal = UserPrincipal.of(userId, role);

                // Authentication 객체 생성
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userPrincipal,  // principal
                                null,           // credentials (JWT는 불필요)
                                null            // authorities (Role은 Principal에 포함)
                        );

                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // SecurityContext에 인증 정보 저장
                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.debug("JWT 인증 성공: userId={}, role={}", userId, role);
            }

        } catch (ApiException e) {
            // JWT 검증 실패 (만료, 위조 등)
            log.warn("JWT 검증 실패: {}", e.getMessage());
            // SecurityContext를 비워서 인증 실패 처리
            SecurityContextHolder.clearContext();
        } catch (Exception e) {
            log.error("JWT 필터 처리 중 오류 발생", e);
            SecurityContextHolder.clearContext();
        }

        // 다음 필터로 진행
        filterChain.doFilter(request, response);
    }

    // Authorization 헤더에서 Bearer 토큰 추출
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7); // "Bearer " 제거
        }

        return null;
    }
}