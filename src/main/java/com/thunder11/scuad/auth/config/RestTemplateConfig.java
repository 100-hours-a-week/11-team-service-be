package com.thunder11.scuad.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

// RestTemplate Bean 설정
// 외부 API 호출을 위한 HTTP 클라이언트
@Configuration
public class RestTemplateConfig {

    // RestTemplate 빈 등록
    // 카카오 OAuth API 등 외부 HTTP 통신에 사용
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}