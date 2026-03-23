package com.thunder11.scuad.infra.redis.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.thunder11.scuad.infra.redis.RedisSubscriber;
import com.thunder11.scuad.infra.redis.SseEventSubscriber;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

// Redis Pub/Sub 설정
//
// 역할 분리 근거:
//   RedisCacheConfig  → 채용공고 목록 캐싱 전용 (데이터 저장/조회)
//   RedisPubSubConfig → 채팅 메시지 브로드캐스트 전용 (다중 서버 간 메시지 전파)
//   두 설정은 사용하는 Redis 기능이 완전히 다르므로 분리하여 변경 영향 범위를 최소화
//
// @ConditionalOnProperty 적용 근거:
//   RedisCacheManager(캐싱)는 실제 캐시 조회 시점에 Redis 연결을 시도하지만
//   RedisMessageListenerContainer(Pub/Sub)는 애플리케이션 시작 시점에 즉시 Redis 연결을 시도함
//   테스트/로컬 환경처럼 Redis가 없는 환경에서 컨텍스트 로드 실패를 방지하기 위해
//   spring.data.redis.pubsub.enabled=true 일 때만 Pub/Sub 설정을 활성화
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.data.redis.pubsub.enabled", havingValue = "true")
public class RedisPubSubConfig {

    private final RedisSubscriber redisSubscriber;
    private final SseEventSubscriber sseEventSubscriber;

    // Pub/Sub 전용 RedisTemplate
    //
    // 용도: ChatMessageService.broadcast()에서 Redis 채널로 메시지 publish
    // 빈 이름을 "chatRedisTemplate"으로 지정한 이유:
    //   Spring Boot가 자동 생성하는 기본 RedisTemplate<Object, Object>와 충돌을 피하기 위함
    //   @Qualifier("chatRedisTemplate")로 주입받아 용도를 명시적으로 구분
    @Bean(name = "chatRedisTemplate")
    public RedisTemplate<String, Object> chatRedisTemplate(RedisConnectionFactory connectionFactory) {
        // JavaTimeModule 등록 근거:
        //   ChatMessageResponse에 LocalDateTime(createdAt) 필드가 있어
        //   기본 GenericJackson2JsonRedisSerializer는 Java 8 날짜 타입을 지원하지 않음
        //   JavaTimeModule을 등록하지 않으면 Redis publish 시 SerializationException 발생
        //   WRITE_DATES_AS_TIMESTAMPS 비활성화: ISO-8601 문자열로 직렬화하여 가독성 확보
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer(objectMapper));
        return template;
    }

    // Redis 메시지 수신 핸들러 어댑터
    //
    // RedisSubscriber.onMessage()를 Redis 메시지 수신 콜백으로 등록
    // "onMessage"는 RedisSubscriber에서 구현한 메서드명과 일치해야 함
    @Bean
    public MessageListenerAdapter messageListenerAdapter() {
        return new MessageListenerAdapter(redisSubscriber, "onMessage");
    }

    // SSE 이벤트 수신 핸들러 어댑터
    //
    // SseEventSubscriber.onMessage()를 chat-sse:* 채널 수신 콜백으로 등록
    // messageListenerAdapter()와 별도 빈으로 분리한 이유:
    //   동일한 컨테이너에 두 리스너를 등록하되 각각 다른 어댑터를 사용해야
    //   채팅 메시지(WebSocket)와 SSE 이벤트 처리가 완전히 독립적으로 동작함
    @Bean
    public MessageListenerAdapter sseMessageListenerAdapter() {
        return new MessageListenerAdapter(sseEventSubscriber, "onMessage");
    }

    // Redis 채널 구독 컨테이너
    //
    // chat-room:* 패턴의 모든 채널 구독 (채팅방 ID별 채널 분리)
    // 메시지 수신 흐름: Redis → messageListenerAdapter → RedisSubscriber.onMessage()
    // PatternTopic 사용 이유:
    //   채팅방이 동적으로 생성되므로 채널명을 미리 알 수 없음
    //   chat-room:* 패턴으로 신규 채팅방 채널을 자동으로 구독
    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            MessageListenerAdapter messageListenerAdapter,
            MessageListenerAdapter sseMessageListenerAdapter
    ) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        // 채팅 메시지 브로드캐스트 채널 (기존)
        container.addMessageListener(messageListenerAdapter, new PatternTopic("chat-room:*"));
        // 채팅방 상태 변경 SSE 알림 채널 (신규)
        // chat-room:* 와 채널 패턴을 분리한 이유:
        //   두 채널의 Subscriber 처리 방식이 완전히 달라(WebSocket vs SSE)
        //   하나의 패턴으로 묶으면 분기 처리 로직이 복잡해지고 변경 영향 범위가 커짐
        container.addMessageListener(sseMessageListenerAdapter, new PatternTopic("chat-sse:*"));
        return container;
    }
}
