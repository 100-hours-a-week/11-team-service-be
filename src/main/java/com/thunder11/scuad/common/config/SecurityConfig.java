package com.thunder11.scuad.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable) // CSRF 보호 비활성화 (API 서버)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/health").permitAll()             // Health Check 허용 (로컬 변경 사항)
                        .requestMatchers("/api/v1/job-postings/**").permitAll() // 채용공고 분석 API 허용 (서버 변경 사항)
                        .anyRequest().authenticated()                           // 그 외 모든 요청은 인증 필요
                );

        return http.build();
    }
}