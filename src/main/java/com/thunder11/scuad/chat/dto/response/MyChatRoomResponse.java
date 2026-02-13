package com.thunder11.scuad.chat.dto.response;

import com.thunder11.scuad.chat.domain.type.MemberRole;
import com.thunder11.scuad.chat.domain.type.RoomGoal;
import com.thunder11.scuad.chat.domain.type.RoomStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// 내가 참여 중인 채팅방 정보 응답 DTO
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MyChatRoomResponse {

    private Long chatRoomId;

    private Long jobMasterId;

    // 채팅방 이름
    private String roomName;

    // 전형 구분 (DOCUMENT: 서류, INTERVIEW: 면접)
    private RoomGoal roomGoal;

    // 커트라인 점수
    private Integer cutlineScore;

    // 현재 인원수
    private Integer currentParticipants;

    // 최대 인원수
    private Integer maxParticipants;

    // 방장 닉네임
    private String hostNickname;

    // 우대 사항
    private String preferredConditions;

    // 채팅방 상태 (ACTIVE, CLOSED)
    private RoomStatus status;

    // 내 역할 (HOST, MEMBER)
    private MemberRole myRole;

    // 마지막 메시지 미리보기 (선택)
    private String lastMessagePreview;

    // 마지막 메시지 시각 (선택, 정렬 기준으로 활용 가능)
    private LocalDateTime lastMessageAt;

    // 채팅방 참여 시각
    private LocalDateTime joinedAt;
}