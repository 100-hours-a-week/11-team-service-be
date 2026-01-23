package com.thunder11.scuad.auth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

// 카카오 OAuth API 호출에 필요한 설정 관리
// application.yml의 kakao.oauth 자동으로 바인딩
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "kakao.oauth")
public class KakaoProperties {

    // 카카오 REST API 키
    private String clientId;

    // 카카오 Client Secret (보안 강화)
    private String clientSecret;

    // 백엔드 콜백 URI (카카오가 인가 코드를 전달할 주소)
    private String redirectUri;

    // 카카오 토큰 발급 엔드포인트
    private String tokenUri;

    // 카카오 사용자 정보 조회 엔드포인트
    private String userInfoUri;
}