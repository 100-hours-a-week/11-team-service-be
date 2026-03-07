package com.thunder11.scuad.common.config;

import java.util.List;
import java.util.Map;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import com.fasterxml.jackson.databind.ObjectMapper;

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

import org.springframework.beans.factory.annotation.Value;

import lombok.RequiredArgsConstructor;

import com.thunder11.scuad.auth.security.JwtAuthenticationFilter;

// Spring Security 설정
// JWT 기반 인증, OAuth 로그인 URL은 퍼블릭 허용
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    // 인증 실패 EntryPoint: 토큰이 없거나 만료된 경우 401 반환
    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) -> {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");
            response.getWriter().write(
                    new ObjectMapper().writeValueAsString(Map.of(
                            "status", 401,
                            "code", "UNAUTHORIZED",
                            "message", "인증이 필요합니다.")));
        };
    }

    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");
            response.getWriter().write(
                    new ObjectMapper().writeValueAsString(Map.of(
                            "status", 403,
                            "code", "FORBIDDEN",
                            "message", "권한이 없습니다.")));
        };
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CORS 설정 활성화
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // CSRF 비활성화 (JWT 사용으로 불필요)
                .csrf(AbstractHttpConfigurer::disable)

                // 세션 사용 안 함 (JWT 기반 Stateless 인증)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // URL별 권한 설정
                .authorizeHttpRequests(auth -> auth
                        // SSE 비동기 해제 등 내부 통신 허용 (이거 없으면 SSE 종료 시 Access Denied 에러 도배 발생)
                        .dispatcherTypeMatchers(jakarta.servlet.DispatcherType.ASYNC,
                                jakarta.servlet.DispatcherType.ERROR, jakarta.servlet.DispatcherType.FORWARD)
                        .permitAll()

                        // 카카오 OAuth 관련 URL은 모두 퍼블릭 허용
                        .requestMatchers("/api/v1/auth/kakao/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/job-postings").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/job-postings/{jobMasterId}").permitAll()
                        // 헬스 체크는 퍼블릭 허용
                        .requestMatchers("/api/health").permitAll()
                        // [로컬 전용] 부하 테스트용 토큰 발급 API
                        .requestMatchers("/api/test/**").permitAll()
                        .requestMatchers("/api/internal/**").permitAll()
                        // Actuator 엔드포인트 퍼블릭 허용
                        .requestMatchers("/actuator/**").permitAll()
                        // 채용공고 조회는 퍼블릭 허용 (임시)
                        .requestMatchers("/api/v1/job-postings/**").permitAll()
                        .requestMatchers("/api/v1/job-postings").permitAll()

                        // 나머지 URL은 인증 필요
                        .anyRequest().authenticated())

                // 기본 로그인 폼 비활성화 (OAuth만 사용)
                .formLogin(AbstractHttpConfigurer::disable)

                // HTTP Basic 인증 비활성화
                .httpBasic(AbstractHttpConfigurer::disable)

                // JWT 인증 필터 추가 (UsernamePasswordAuthenticationFilter 앞에 배치)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

                // 인증/인가 실패 시 커스텀 핸들러 등록
                // - 401: 토큰 없음/만료 (프론트에서 자동 재발급 시도 트리거)
                // - 403: 인증은 됐지만 권한 없음 (재발급 없이 에러 처리)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint())
                        .accessDeniedHandler(accessDeniedHandler()));

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(frontendUrl));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}