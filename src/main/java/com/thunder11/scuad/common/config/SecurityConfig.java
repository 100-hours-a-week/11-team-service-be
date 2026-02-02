package com.thunder11.scuad.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import lombok.RequiredArgsConstructor;

import com.thunder11.scuad.auth.security.JwtAuthenticationFilter;

import java.util.Arrays;

// Spring Security 설정
// JWT 기반 인증, OAuth 로그인 URL은 퍼블릭 허용
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CORS 설정 추가
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // CSRF 비활성화 (JWT 사용으로 불필요)
                .csrf(AbstractHttpConfigurer::disable)

                // 세션 사용 안 함 (JWT 기반 Stateless 인증)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // URL별 권한 설정
                .authorizeHttpRequests(auth -> auth
                        // 카카오 OAuth 관련 URL은 모두 퍼블릭 허용
                        .requestMatchers("/api/v1/auth/kakao/**").permitAll()
                        // 헬스 체크는 퍼블릭 허용
                        .requestMatchers("/api/health").permitAll()

                        // 나머지 URL은 인증 필요
                        .anyRequest().authenticated()
                )

                // 기본 로그인 폼 비활성화 (OAuth만 사용)
                .formLogin(AbstractHttpConfigurer::disable)

                // HTTP Basic 인증 비활성화
                .httpBasic(AbstractHttpConfigurer::disable)

                // JWT 인증 필터 추가 (UsernamePasswordAuthenticationFilter 앞에 배치)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // CORS 설정
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 허용할 Origin 설정
        configuration.setAllowedOrigins(Arrays.asList(
                "https://scuad.kr",           // 운영: www 없는 도메인
                "https://www.scuad.kr",       // 운영: www 있는 도메인 (CORS 에러 해결)
                "http://localhost:3000",      // 개발: 프론트엔드
                "http://localhost:8080"       // 개발: 백엔드 (테스트용)
        ));

        // 허용할 HTTP 메서드
        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
        ));

        // 허용할 헤더 (Authorization, Content-Type 등)
        configuration.setAllowedHeaders(Arrays.asList("*"));

        // 인증 정보 포함 허용 (쿠키, Authorization 헤더)
        // Refresh Token이 HttpOnly 쿠키로 전달되므로 필수
        configuration.setAllowCredentials(true);

        // Preflight 요청 캐싱 시간 (1시간)
        // OPTIONS 요청 반복을 줄여 성능 향상
        configuration.setMaxAge(3600L);

        // 모든 경로에 CORS 설정 적용
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}