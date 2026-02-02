package com.thunder11.scuad.chat.dto.response;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Getter;

import com.thunder11.scuad.chat.domain.type.RoomGoal;
import com.thunder11.scuad.chat.domain.type.RoomStatus;

// 채팅방 상세 정보 조회 응답
@Getter
@Builder
public class ChatRoomDetailResponse {

    private Long chatRoomId;
    private String roomName;
    private RoomGoal roomGoal;
    private Integer cutlineScore;
    private Integer currentParticipants;
    private Integer maxParticipants;
    private String hostNickname;
    private String preferredConditions;
    private RoomStatus status;

    // 공고 요약 정보
    private JobMasterSummary jobMaster;

    // 멤버 요약 정보
    private Integer memberCount;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    // 공고 요약 정보 내부 클래스
    @Getter
    @Builder
    public static class JobMasterSummary {
        private Long jobMasterId;
        private String companyName;
        private String jobTitle;
    }
}