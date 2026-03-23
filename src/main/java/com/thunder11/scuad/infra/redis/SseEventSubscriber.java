package com.thunder11.scuad.infra.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thunder11.scuad.chat.dto.ChatSseEvent;
import com.thunder11.scuad.chat.repository.ChatRoomMemberRepository;
import com.thunder11.scuad.notification.service.SseEmitterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

/**
 * Redis chat-sse:* 채널 구독자
 *
 * 역할: Redis 채널(chat-sse:{chatRoomId})에서 이벤트를 수신하여
 *       해당 채팅방의 활성 멤버 전원에게 SSE push
 *
 * 기존 RedisSubscriber와 분리한 이유:
 *   RedisSubscriber → WebSocket 브로드캐스트 (채팅 메시지 전달용)
 *   SseEventSubscriber → SSE push (채팅방 상태 변경 알림용)
 *   두 클래스의 처리 방식이 완전히 달라 하나의 클래스에 합치면 단일 책임 원칙을 위반하고
 *   이후 변경 시 영향 범위를 파악하기 어려워짐.
 *
 * 다중 서버 이벤트 전파 흐름:
 *   서버B에서 joinChatRoom() 실행
 *     → SseEventPublisher.publishMemberJoined() → Redis publish("chat-sse:1")
 *                                                          ↓
 *                                                   Redis Pub/Sub
 *                                                          ↓
 *                     서버A SseEventSubscriber.onMessage() → 서버A 연결 클라이언트 push ✅
 *                     서버B SseEventSubscriber.onMessage() → 서버B 연결 클라이언트 push ✅
 *
 * Map 대신 ChatSseEvent로 역직렬화하는 이유:
 *   ChatSseEvent는 @Builder만 선언되어 있어 기본 생성자가 없으므로
 *   RedisSubscriber처럼 Map<String, Object>로 역직렬화한 뒤
 *   ObjectMapper로 ChatSseEvent로 변환하는 방식을 사용한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SseEventSubscriber implements MessageListener {

    private final ObjectMapper objectMapper;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final SseEmitterRegistry sseEmitterRegistry;

    public void onMessage(Message message, byte[] pattern) {
        try {
            String channel = new String(message.getChannel());
            Long chatRoomId = Long.parseLong(channel.replace("chat-sse:", ""));

            ChatSseEvent event = objectMapper.readValue(message.getBody(), ChatSseEvent.class);

            // 해당 채팅방의 활성 멤버 전원에게 SSE push
            // 다중 서버 환경에서는 각 서버가 자신에게 연결된 Emitter만 보유하므로
            // sendChatEvent() 내부에서 emitters.get(userId)가 null이면 조용히 스킵됨
            chatRoomMemberRepository.findAllActiveMembersByChatRoomId(chatRoomId)
                    .forEach(member -> sseEmitterRegistry.sendChatEvent(member.getUserId(), event));

            log.info("SSE 이벤트 브로드캐스트 완료: chatRoomId={}, type={}", chatRoomId, event.getType());

        } catch (Exception e) {
            log.error("SSE 이벤트 처리 실패: channel={}, error={}",
                    new String(message.getChannel()), e.getMessage(), e);
        }
    }
}
