package com.thunder11.scuad.auth.config;

import com.nimbusds.jwt.JWT;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

import java.util.Properties;

// JWT 설정 Properties로 application.yml의 jwt 설정을 Java 객체로 매핑하는 역할
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {


    // JWT에 사용할 비밀키, 환경 변수로 관리
    private String secret;

    private long accessTokenExpiration;
    private long refreshTokenExpiration;
}