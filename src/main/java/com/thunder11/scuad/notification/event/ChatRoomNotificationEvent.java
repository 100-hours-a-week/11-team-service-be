package com.thunder11.scuad.notification.event;

/**
 * 채팅방 관련 알림 이벤트
 *
 * AiAnalysisCompleteEvent와 동일한 패턴으로 설계.
 * ChatRoomService에서 publishEvent()로 발행하고,
 * NotificationService의 @TransactionalEventListener(AFTER_COMMIT)가 수신하여
 * 트랜잭션 커밋 이후 알림을 발송한다.
 *
 * @param userId          알림 수신 대상 userId
 * @param type            알림 타입 (CHAT_ROOM_KICKED / CHAT_ROOM_CLOSED)
 * @param chatRoomName    알림 본문에 사용할 채팅방 이름
 * @param chatRoomId      refId로 사용할 채팅방 ID
 */
public record ChatRoomNotificationEvent(
        Long userId,
        String type,
        String chatRoomName,
        Long chatRoomId
) {}
