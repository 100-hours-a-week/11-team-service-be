package com.thunder11.scuad.notification.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import lombok.extern.slf4j.Slf4j;

import com.thunder11.scuad.notification.dto.request.SseNotificationEvent;

@Slf4j
@Component
public class SseEmitterRegistry {
    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter register(Long userId) {
        SseEmitter existing = emitters.get(userId);
        if (existing != null) {
            existing.complete();
        }

        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);
        emitters.put(userId, emitter);

        emitter.onCompletion(() -> {
            boolean removed = emitters.remove(userId, emitter);
            if (removed) {
                log.info("SSE 연결 종료(완료): userId={}", userId);
            }
        });
        emitter.onTimeout(() -> {
            boolean removed = emitters.remove(userId, emitter);
            if (removed) {
                log.info("SSE 연결 타임아웃: userId={}", userId);
            }
        });
        emitter.onError((e) -> {
            boolean removed = emitters.remove(userId, emitter);
            if (removed) {
                log.warn("SSE 연결 에러: userId={}, message={}", userId, e.getMessage());
            }
        });

        try {
            emitter.send(SseEmitter.event().name("connect").data("connected successfully"));
        } catch (Exception e) {
            log.warn("첫 SSE 커넥션 데이터 전송 실패: userId={}", userId);
        }
        return emitter;
    }

    public void send(Long userId, SseNotificationEvent event) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name("notification")
                        .data(event)
                        .id(String.valueOf(event.getNotificationId())));
                log.info("SSE 토스트 발송 완료 userId={}, notificationId={}", userId, event.getNotificationId());
            } catch (Exception e) {
                log.warn("전송중 브라우저 닫힘 등의 사유로 에러 발생. 커넥션 해제 userId={}, type={}", userId, event.getType());
                emitters.remove(userId, emitter);
            }
        } else {
            log.debug("오프라인 유저입니다. DB에만 저장됩니다. userId={}", userId);
        }
    }
}