package com.thunder11.scuad.infra.redis.config;

import com.thunder11.scuad.infra.redis.RedisSubscriber;
import lombok.RequiredArgsConstructor;
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
@Configuration
@RequiredArgsConstructor
public class RedisPubSubConfig {

    private final RedisSubscriber redisSubscriber;

    // Pub/Sub 전용 RedisTemplate
    //
    // 용도: ChatMessageService.broadcast()에서 Redis 채널로 메시지 publish
    // 빈 이름을 "chatRedisTemplate"으로 지정한 이유:
    //   Spring Boot가 자동 생성하는 기본 RedisTemplate<Object, Object>와 충돌을 피하기 위함
    //   @Qualifier("chatRedisTemplate")로 주입받아 용도를 명시적으로 구분
    @Bean(name = "chatRedisTemplate")
    public RedisTemplate<String, Object> chatRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        // key: 채널명 문자열 (예: chat-room:1) → StringRedisSerializer
        template.setKeySerializer(new StringRedisSerializer());
        // value: ChatMessageResponse JSON 직렬화 → GenericJackson2JsonRedisSerializer
        //        역직렬화 시 @class 필드로 타입을 복원하므로 별도 타입 지정 불필요
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
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
            MessageListenerAdapter messageListenerAdapter
    ) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(messageListenerAdapter, new PatternTopic("chat-room:*"));
        return container;
    }
}
