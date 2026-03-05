package com.thunder11.scuad.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
// proxyTargetClass=true 추가 이유:
// AiServiceClient가 AiComparePort 인터페이스를 구현하면서
// Spring이 JDK 동적 프록시로 감싸 AiServiceClient 타입으로 주입이 불가능해짐.
// CGLib 기반 프록시로 강제하면 구체 클래스 타입으로도 주입 가능.
@EnableAsync(proxyTargetClass = true)
public class AsyncConfig {
}
