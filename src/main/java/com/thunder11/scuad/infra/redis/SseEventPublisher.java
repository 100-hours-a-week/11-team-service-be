package com.thunder11.scuad.infra.redis;

import com.thunder11.scuad.chat.dto.ChatSseEvent;
import com.thunder11.scuad.chat.repository.ChatRoomMemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 채팅방 상태 변경 이벤트를 Redis 채널에 publish하는 컴포넌트
 *
 * 호출 위치: ChatRoomController — Service 메서드 리턴(= 트랜잭션 커밋 완료) 후 즉시 호출
 *
 * 트랜잭션 커밋 후에 호출해야 하는 이유:
 *   @Transactional 메서드 내부에서 publish하면 커밋 전에 이벤트가 나가므로
 *   클라이언트가 이벤트 수신 후 즉시 멤버 목록/인원수를 조회하면
 *   DB에 아직 반영되지 않은 데이터를 받는 레이스 컨디션이 발생한다.
 *   ChatMessageService.broadcast()와 동일한 원칙을 따른다.
 *
 * Redis 채널명 규칙: chat-sse:{chatRoomId}
 *   기존 채팅 메시지 채널(chat-room:*)과 분리하여
 *   SseEventSubscriber가 WebSocket 브로드캐스트 없이 SSE push만 처리하도록 역할을 명확히 구분
 *
 * @ConditionalOnProperty 미적용 이유:
 *   chatRedisTemplate이 @Autowired(required = false)로 주입되므로
 *   Redis 비활성화 환경에서 빈이 없어도 컨텍스트 로드에 문제없음.
 *   publish() 호출 시 null 체크로 안전하게 처리.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SseEventPublisher {

    private final ChatRoomMemberRepository chatRoomMemberRepository;

    // required = false 적용 근거:
    //   RedisPubSubConfig가 @ConditionalOnProperty로 조건부 로드되므로
    //   Redis 비활성화 환경(테스트, 로컬)에서는 chatRedisTemplate 빈이 존재하지 않음.
    //   ChatMessageService.broadcast()와 동일한 패턴으로 처리.
    @Autowired(required = false)
    @Qualifier("chatRedisTemplate")
    private RedisTemplate<String, Object> redisTemplate;

    public void publishMemberJoined(Long chatRoomId) {
        long currentParticipants = chatRoomMemberRepository.countByChatRoomIdAndKickedAtIsNull(chatRoomId);
        publish(chatRoomId, ChatSseEvent.builder()
                .type("MEMBER_JOINED")
                .chatRoomId(chatRoomId)
                .currentParticipants((int) currentParticipants)
                .build());
    }

    public void publishMemberLeft(Long chatRoomId) {
        long currentParticipants = chatRoomMemberRepository.countByChatRoomIdAndKickedAtIsNull(chatRoomId);
        publish(chatRoomId, ChatSseEvent.builder()
                .type("MEMBER_LEFT")
                .chatRoomId(chatRoomId)
                .currentParticipants((int) currentParticipants)
                .build());
    }

    public void publishMemberKicked(Long chatRoomId, Long kickedUserId) {
        long currentParticipants = chatRoomMemberRepository.countByChatRoomIdAndKickedAtIsNull(chatRoomId);
        publish(chatRoomId, ChatSseEvent.builder()
                .type("MEMBER_KICKED")
                .chatRoomId(chatRoomId)
                .currentParticipants((int) currentParticipants)
                .kickedUserId(kickedUserId)
                .build());
    }

    public void publishRoomClosed(Long chatRoomId) {
        publish(chatRoomId, ChatSseEvent.builder()
                .type("ROOM_CLOSED")
                .chatRoomId(chatRoomId)
                .build());
    }

    private void publish(Long chatRoomId, ChatSseEvent event) {
        if (redisTemplate == null) {
            log.warn("Redis Pub/Sub 비활성화 상태 — SSE 이벤트 publish 스킵: chatRoomId={}, type={}",
                    chatRoomId, event.getType());
            return;
        }
        String channel = "chat-sse:" + chatRoomId;
        redisTemplate.convertAndSend(channel, event);
        log.info("SSE 이벤트 publish 완료: channel={}, type={}", channel, event.getType());
    }
}
