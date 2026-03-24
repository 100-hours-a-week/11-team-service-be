package com.thunder11.scuad.infra.redis;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

// Redis Pub/Sub 구독자
//
// 역할: Redis 채널(chat-room:{chatRoomId})에서 메시지를 수신하여
//       이 서버에 WebSocket으로 연결된 클라이언트(/topic/chat-rooms/{chatRoomId})에게 브로드캐스트
//
// 다중 서버 메시지 전파 흐름:
//   클라이언트 전송
//     → 서버1 HTTP 수신 → Redis publish(chat-room:1)
//                               ↓
//                          Redis Pub/Sub
//                               ↓
//              서버1 onMessage() → WebSocket → 서버1 구독 클라이언트 ✅
//              서버2 onMessage() → WebSocket → 서버2 구독 클라이언트 ✅
//
// Map<String, Object>로 역직렬화하는 이유:
//   ChatMessageResponse는 @Builder만 선언되어 있어 Jackson 역직렬화 시
//   기본 생성자가 없으면 역직렬화 실패 가능성이 있음.
//   수신한 JSON을 Map으로 읽어 그대로 WebSocket으로 전달하면
//   클라이언트가 받는 JSON 구조는 동일하게 유지되면서 역직렬화 안전성을 확보
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisSubscriber implements MessageListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    // Redis 채널 메시지 수신 콜백
    //
    // message.getChannel(): 메시지가 발행된 채널명 바이트 (예: chat-room:1)
    // message.getBody()   : JSON 직렬화된 ChatMessageResponse 바이트
    //
    // 채널명에서 chatRoomId를 추출하는 이유:
    //   PatternTopic("chat-room:*")으로 모든 채팅방 채널을 하나의 리스너로 처리하므로
    //   어느 채팅방의 메시지인지 채널명에서 파싱하여 올바른 WebSocket 주제로 전달
    public void onMessage(Message message, byte[] pattern) {
        try {
            String channel = new String(message.getChannel());
            String chatRoomId = channel.replace("chat-room:", "");

            // Map으로 역직렬화 → ChatMessageResponse의 JSON 구조를 그대로 보존
            Map<String, Object> messageData = objectMapper.readValue(
                    message.getBody(),
                    new TypeReference<Map<String, Object>>() {
                    }
            );

            messagingTemplate.convertAndSend("/topic/chat-rooms/" + chatRoomId, messageData);
            log.info("Redis → WebSocket 브로드캐스트 완료: chatRoomId={}, messageId={}",
                    chatRoomId, messageData.get("messageId"));

        } catch (Exception e) {
            log.error("Redis 메시지 처리 실패: channel={}, error={}",
                    new String(message.getChannel()), e.getMessage(), e);
        }
    }
}
