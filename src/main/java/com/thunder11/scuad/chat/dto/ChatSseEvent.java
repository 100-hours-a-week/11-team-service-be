package com.thunder11.scuad.chat.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 채팅방 상태 변경 SSE 이벤트 DTO
 *
 * SseEventPublisher → Redis publish → SseEventSubscriber → SseEmitterRegistry.sendChatEvent()
 * 흐름으로 전달되는 페이로드.
 *
 * currentParticipants: MEMBER_JOINED / MEMBER_LEFT / MEMBER_KICKED 에서 사용.
 *                      ROOM_CLOSED는 인원수가 의미 없으므로 null로 전달.
 * kickedUserId:        MEMBER_KICKED 에서만 사용.
 *                      클라이언트가 본인이 강퇴 대상인지 판별하는 데 사용.
 *                      다른 이벤트 타입에서는 null.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatSseEvent {
    private String type;
    private Long chatRoomId;
    private Integer currentParticipants;
    private Long kickedUserId;
}
