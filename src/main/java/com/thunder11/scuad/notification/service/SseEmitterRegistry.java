package com.thunder11.scuad.notification.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import lombok.extern.slf4j.Slf4j;

import com.thunder11.scuad.notification.dto.request.SseNotificationEvent;
import com.thunder11.scuad.chat.dto.ChatSseEvent;

@Slf4j
@Component
public class SseEmitterRegistry {
    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter register(Long userId) {
        SseEmitter existing = emitters.get(userId);
        if (existing != null) {
            try {
                existing.complete();
            } catch (Exception e) {
            }
        }

        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);
        emitters.put(userId, emitter);

        // 초기 연결 성공 메시지 전송 (연결 유지를 위함)
        try {
            synchronized (emitter) {
                emitter.send(SseEmitter.event()
                        .name("connect")
                        .data("connected successfully"));
            }
        } catch (Exception e) {
            emitters.remove(userId, emitter);
        }

        emitter.onCompletion(() -> emitters.remove(userId, emitter));
        emitter.onTimeout(() -> emitters.remove(userId, emitter));
        emitter.onError((e) -> emitters.remove(userId, emitter));

        return emitter;
    }

    @Scheduled(fixedRate = 15000)
    public void sendHeartbeat() {
        if (emitters.isEmpty()) return;

        emitters.forEach((userId, emitter) -> {
            try {
                synchronized (emitter) {
                    emitter.send(SseEmitter.event()
                            .name("heartbeat")
                            .data("keep-alive"));
                }
            } catch (Exception e) {
                emitters.remove(userId, emitter);
            }
        });
    }

    public void send(Long userId, SseNotificationEvent event) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter != null) {
            try {
                synchronized (emitter) {
                    emitter.send(SseEmitter.event()
                            .name("notification")
                            .data(event)
                            .id(String.valueOf(event.getNotificationId())));
                }
                log.info("SSE 토스트 발송 완료 userId={}, notificationId={}", userId, event.getNotificationId());
            } catch (Exception e) {
                log.warn("SSE 전송 실패. 커넥션 해제 userId={}", userId);
                emitters.remove(userId, emitter);
            }
        }
    }

    // 채팅방 상태 변경 이벤트 전송
    //
    // 기존 send()와 분리한 이유:
    //   기존 send()는 이벤트명이 "notification"으로 하드코딩되어 있어
    //   클라이언트가 알림 토스트와 채팅방 상태 변경을 이벤트명으로 구분할 수 없음.
    //   "chat-room-update"라는 별도 이벤트명을 사용하여
    //   클라이언트가 EventSource 리스너에서 용도별로 분기 처리할 수 있도록 함.
    public void sendChatEvent(Long userId, ChatSseEvent event) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter != null) {
            try {
                synchronized (emitter) {
                    emitter.send(SseEmitter.event()
                            .name("chat-room-update")
                            .data(event));
                }
                log.info("채팅방 SSE 이벤트 발송 완료: userId={}, type={}, chatRoomId={}",
                        userId, event.getType(), event.getChatRoomId());
            } catch (Exception e) {
                log.warn("채팅방 SSE 전송 실패. 커넥션 해제: userId={}", userId);
                emitters.remove(userId, emitter);
            }
        }
    }
}