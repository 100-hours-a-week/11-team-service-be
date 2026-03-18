package com.thunder11.scuad.config;

import org.mockito.Mockito;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * 테스트 환경 인프라 Mock 설정
 *
 * <p>목적: @SpringBootTest 로드 시 Redis, RabbitMQ, AWS S3 등 외부 인프라에
 * 실제로 연결하려다 실패하는 것을 방지한다.
 *
 * <p>Mock 빈 등록 전략:
 * RabbitAutoConfiguration을 exclude하면 RabbitTemplate 빈 자체가 사라져서
 * AiEvaluationWorker 등 RabbitTemplate을 주입받는 서비스가 NoSuchBeanDefinitionException으로 실패한다.
 * 따라서 자동 구성을 제외하는 대신, ConnectionFactory와 RabbitTemplate 모두 Mock으로 등록하여
 * 자동 구성이 Mock 빈을 사용하도록 유도한다.
 *
 * <p>@Primary 사용 이유:
 * Spring Boot 자동 구성 빈과 동시에 등록될 때 충돌을 피하기 위해
 * 명시적으로 이 빈을 우선 주입 대상으로 지정한다.
 *
 * <p>적용 대상:
 * - RedisConnectionFactory : RedisCacheConfig, RedisPubSubConfig 의존
 * - ConnectionFactory      : spring-boot-starter-amqp 자동 구성 의존
 * - RabbitTemplate         : AiEvaluationWorker 및 기타 서비스 직접 주입
 * - S3Client               : S3FileManagementService 의존
 */
@TestConfiguration
public class TestInfraConfig {

    /**
     * Redis 연결 팩토리 Mock
     *
     * RedisCacheConfig.cacheManager()와 RedisPubSubConfig가
     * RedisConnectionFactory를 주입받으므로 Mock으로 대체한다.
     * LettuceConnectionFactory로 Mock하는 이유:
     *   Spring Boot 자동 구성이 기본으로 Lettuce를 사용하므로
     *   타입 불일치 없이 @Primary로 교체할 수 있도록 구체 타입으로 Mock한다.
     */
    @Bean
    @Primary
    public RedisConnectionFactory redisConnectionFactory() {
        return Mockito.mock(LettuceConnectionFactory.class);
    }

    /**
     * RabbitMQ 연결 팩토리 Mock
     *
     * spring-boot-starter-amqp 자동 구성이 이 빈을 사용한다.
     * RabbitAutoConfiguration을 exclude하지 않고 연결 팩토리만 Mock으로 교체하면
     * RabbitTemplate 등 나머지 AMQP 빈들이 정상적으로 자동 구성된다.
     */
    @Bean
    @Primary
    public ConnectionFactory rabbitConnectionFactory() {
        return Mockito.mock(ConnectionFactory.class);
    }

    /**
     * RabbitTemplate Mock
     *
     * AiEvaluationWorker 등 RabbitTemplate을 직접 주입받는 서비스가 있다.
     * ConnectionFactory Mock만으로는 RabbitTemplate 생성 시 실제 브로커 연결을 시도하므로
     * RabbitTemplate 자체도 Mock으로 등록하여 연결 시도를 완전히 차단한다.
     */
    @Bean
    @Primary
    public RabbitTemplate rabbitTemplate() {
        return Mockito.mock(RabbitTemplate.class);
    }

    /**
     * AWS S3 클라이언트 Mock
     *
     * S3FileManagementService에 주입되는 S3Client를 Mock으로 대체한다.
     * spring-cloud-aws-starter-s3 자동 구성이 AWS 자격증명을 찾지 못해
     * 컨텍스트 로드에 실패하는 것을 방지한다.
     */
    @Bean
    @Primary
    public S3Client s3Client() {
        return Mockito.mock(S3Client.class);
    }
}
