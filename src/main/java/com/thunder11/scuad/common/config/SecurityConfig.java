package com.thunder11.scuad.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

// Spring Security 설정
// OAuth 로그인 URL은 퍼블릭 허용, JWT 기반 인증 사용
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
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
                .httpBasic(AbstractHttpConfigurer::disable);

        return http.build();
    }
}
