package com.thunder11.scuad.notification.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import com.thunder11.scuad.auth.domain.User;
import com.thunder11.scuad.auth.repository.UserRepository;
import com.thunder11.scuad.notification.domain.Notification;
import com.thunder11.scuad.notification.dto.request.SseNotificationEvent;
import com.thunder11.scuad.notification.dto.response.NotificationResponse;
import com.thunder11.scuad.notification.repository.NotificationRepository;
import com.thunder11.scuad.notification.event.AiAnalysisCompleteEvent;
import com.thunder11.scuad.notification.event.ChatRoomNotificationEvent;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final SseEmitterRegistry sseEmitterRegistry;
    private final UserRepository userRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleAiAnalysisComplete(AiAnalysisCompleteEvent event) {
        try {
            createAndPush(event.userId(), event.type(), event.jobPostingTitle(), event.applicationId());
        } catch (Exception e) {
            log.error("알림 발송 오류: {}", e.getMessage());
        }
    }

    // 채팅방 강퇴 / 종료 알림 핸들러
    //
    // AiAnalysisCompleteEvent 핸들러와 동일한 패턴으로 구현.
    // ChatRoomService의 @Transactional 메서드 내부에서 publishEvent()를 호출하면
    // 트랜잭션 커밋 이후 이 핸들러가 수신하여 알림을 발송한다.
    // REQUIRES_NEW를 사용하는 이유: 알림 발송 실패가 채팅방 비즈니스 로직 트랜잭션에
    // 영향을 주지 않도록 독립된 트랜잭션으로 처리한다.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleChatRoomNotification(ChatRoomNotificationEvent event) {
        try {
            createAndPush(event.userId(), event.type(), event.chatRoomName(), event.chatRoomId());
        } catch (Exception e) {
            log.error("채팅방 알림 발송 오류: userId={}, type={}, error={}",
                    event.userId(), event.type(), e.getMessage());
        }
    }

    @Transactional
    public void createAndPush(Long userId, String type, String jobPostingTitle, Long applicationId) {
        User user = userRepository.findById(userId).orElseThrow();

        String safeJobTitle = jobPostingTitle;
        if (safeJobTitle != null && safeJobTitle.length() > 15) {
            safeJobTitle = safeJobTitle.substring(0, 15) + "...";
        }
        String title;
        String body;
        String refType = "APPLICATION";

        switch (type) {
            case "JOB_POSTING_COMPLETE" -> {
                title = "공고 분석 완료";
                body = "채용공고 분석이 완료되었습니다. 지금 확인해 보세요!";
                refType = "JOBPOSTING";
            }
            case "AI_EVAL_COMPLETE" -> {
                title = safeJobTitle + "평가 완료";
                body = "평가가 완료된 내 점수를 확인하세요";
            }
            case "RESUME_COMPLETE" -> {
                title = safeJobTitle + " 이력서 분석";
                body = "이력서 분석 결과를 확인하세요!";
            }
            case "PORTFOLIO_COMPLETE" -> {
                title = safeJobTitle + " 포트폴리오 분석";
                body = "포트폴리오 분석 결과를 확인하세요!";
            }
            case "COMPARISON_COMPLETE" -> {
                title = safeJobTitle + " 비교 분석 완료";
                body = "AI 비교 분석 결과를 확인해보세요.";
            }
            case "CHAT_ROOM_KICKED" -> {
                title = "채팅방에서 강퇴되었습니다.";
                body = safeJobTitle + " 채팅방에서 내보내졌습니다.";
                refType = "CHAT_ROOM";
            }
            case "CHAT_ROOM_CLOSED" -> {
                title = "채팅방이 종료되었습니다.";
                body = safeJobTitle + " 채팅방이 종료되었습니다.";
                refType = "CHAT_ROOM";
            }
            default -> {
                title = safeJobTitle + " 시스템 알림";
                body = "처리가 완료되었습니다.";
            }
        }

        Notification notification = Notification.builder()
                .user(user)
                .title(title)
                .body(body)
                .type(type)
                .refType(refType)
                .refId(applicationId)
                .isRead(false)
                .build();

        Notification saved = notificationRepository.save(notification);

        sseEmitterRegistry.send(userId, SseNotificationEvent.builder()
                .notificationId(saved.getId())
                .type(type)
                .title(title)
                .body(body)
                .refId(applicationId)
                .build());
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotifications(Long userId) {
        return notificationRepository.findByUserUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(NotificationResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserUserIdAndIsReadFalse(userId);
    }

    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findByIdAndUserUserId(notificationId, userId)
                .orElseThrow(() -> new IllegalArgumentException("권한없음/알 수 없는 알림"));
        notification.markAsRead();
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsRead(userId);
    }

    @Transactional
    public void delete(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findByIdAndUserUserId(notificationId, userId)
                .orElseThrow(() -> new IllegalArgumentException("권한없음/알 수 없는 알림"));
        notificationRepository.delete(notification);
    }
}
