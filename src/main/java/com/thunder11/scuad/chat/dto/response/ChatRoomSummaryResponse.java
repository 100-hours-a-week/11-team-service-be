package com.thunder11.scuad.chat.dto.response;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Getter;

import com.thunder11.scuad.chat.domain.type.RoomGoal;
import com.thunder11.scuad.chat.domain.type.RoomStatus;

// 채팅방 목록 조회 시 사용하는 요약 정보
@Getter
@Builder
public class ChatRoomSummaryResponse {

    private Long chatRoomId;
    private String roomName;
    private RoomGoal roomGoal;
    private Integer cutlineScore;
    private Integer currentParticipants;
    private Integer maxParticipants;
    private String hostNickname;
    private String preferredConditions;
    private RoomStatus status;

    // 입장 가능 여부
    private Boolean canJoin;

    // 입장 상태 (FULL, AVAILABLE, ALREADY_JOINED 등)
    private String joinStatus;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}