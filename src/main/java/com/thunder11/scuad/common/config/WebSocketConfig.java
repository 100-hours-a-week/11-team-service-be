package com.thunder11.scuad.common.config;

import com.thunder11.scuad.chat.websocket.WebSocketAuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

// WebSocket + STOMP 설정
// 클라이언트가 /ws 로 연결하면 STOMP 프로토콜로 메시지를 주고받을 수 있음
// 폴링(2초마다 GET 반복) → WebSocket(서버가 변경 시에만 push) 전환을 위해 추가
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketAuthInterceptor webSocketAuthInterceptor;

    // SecurityConfig의 CORS 설정(frontendUrl만 허용)과 일치시키기 위해 동일한 프로퍼티 주입
    // 기존 setAllowedOriginPatterns("*")는 운영 환경에서 모든 오리진의 WebSocket 연결을 허용하는
    // 보안 취약점이 있어 수정
    @Value("${app.frontend-url}")
    private String frontendUrl;
    // 메시지 브로커 설정
    // enableSimpleBroker("/topic"): /topic/chat-rooms/{id} 를 구독한 클라이언트들에게 메시지 브로드캐스트
    // setApplicationDestinationPrefixes("/app"): 클라이언트가 서버로 메시지 보낼 때 사용하는 prefix
    //   예) 클라이언트가 /app/chat-rooms/1/messages 로 전송 → @MessageMapping("/chat-rooms/1/messages") 가 수신
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    // STOMP 엔드포인트 등록
    // addEndpoint("/ws"): 클라이언트가 WebSocket 연결을 맺는 URL (ws://서버주소/ws)
    // setAllowedOriginPatterns("*"): CORS 허용 (운영에서는 프론트엔드 도메인으로 제한 필요)
    // withSockJS(): WebSocket 미지원 환경에서 자동으로 Long Polling 등으로 폴백
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .addInterceptors(webSocketAuthInterceptor)  // 핸드셰이크 시 JWT 검증
                .setAllowedOriginPatterns(frontendUrl)      // SecurityConfig CORS와 동일하게 제한
                .withSockJS();
    }
}