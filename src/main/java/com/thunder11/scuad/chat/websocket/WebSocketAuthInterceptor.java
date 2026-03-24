package com.thunder11.scuad.chat.websocket;

import com.thunder11.scuad.auth.util.JwtProvider;
import com.thunder11.scuad.common.exception.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

// WebSocket 핸드셰이크 시점의 JWT 인증 인터셉터
// WebSocket은 한 번 연결되면 HTTP 헤더를 다시 보낼 수 없기 때문에
// 최초 연결(핸드셰이크) 시점에 JWT를 검증하고 userId를 세션에 저장해둠
// 이후 @MessageMapping 핸들러에서 세션에 저장된 userId를 꺼내 사용
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements HandshakeInterceptor {

    private final JwtProvider jwtProvider;

    // 핸드셰이크 전 처리 — JWT 검증 및 userId 세션 저장
    // 클라이언트는 연결 URL 쿼리 파라미터로 토큰 전달
    // 예: ws://localhost:8080/ws?token=eyJhbGci...
    // SockJS 환경에서는 HTTP 헤더 설정이 제한적이라 쿼리 파라미터 방식 사용
    // return true: 연결 허용 / return false: 연결 거절
    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes
    ) {
        try {
            // 쿼리 파라미터에서 token 추출
            String query = request.getURI().getQuery();
            if (query == null || !query.contains("token=")) {
                log.warn("WebSocket 연결 거절 — 토큰 없음: uri={}", request.getURI());
                return false;
            }

            // token= 이후 값 파싱
            String token = null;
            for (String param : query.split("&")) {
                if (param.startsWith("token=")) {
                    token = param.substring("token=".length());
                    break;
                }
            }

            if (token == null || token.isBlank()) {
                log.warn("WebSocket 연결 거절 — 토큰 값 없음");
                return false;
            }

            // JWT 유효성 검증 (만료/위조 시 ApiException 발생)
            jwtProvider.validateToken(token);

            // userId 추출 후 WebSocket 세션에 저장
            // 세션에 저장해두면 이후 메시지 처리 시 DB 조회 없이 바로 사용 가능
            Long userId = jwtProvider.getUserIdFromToken(token);
            attributes.put("userId", userId);

            log.info("WebSocket 핸드셰이크 성공: userId={}", userId);
            return true;

        } catch (ApiException e) {
            log.warn("WebSocket 연결 거절 — 토큰 인증 실패: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("WebSocket 핸드셰이크 중 예외 발생", e);
            return false;
        }
    }

    // 핸드셰이크 후 처리 — 현재는 별도 처리 없음
    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception
    ) {
    }
}