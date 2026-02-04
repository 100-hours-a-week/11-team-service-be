package com.thunder11.scuad.chat.domain;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

import com.thunder11.scuad.chat.domain.type.RoomGoal;
import com.thunder11.scuad.chat.domain.type.RoomStatus;
import com.thunder11.scuad.common.entity.BaseTimeEntity;

// 채용공고 기준으로 생성되는 채팅방
@Entity
@Table(name = "chat_rooms")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoom extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chat_room_id")
    private Long chatRoomId;

    // 소속 통합 공고 (job_masters)
    @Column(name = "job_master_id", nullable = false)
    private Long jobMasterId;

    // 방장 (users)
    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    // 채팅방 이름
    @Column(name = "room_name", nullable = false, length = 50)
    private String roomName;

    // 최대 인원 (2~5)
    @Column(name = "max_participants", nullable = false)
    private Integer maxParticipants;

    // 채팅방 목표
    @Enumerated(EnumType.STRING)
    @Column(name = "room_goal", nullable = false, length = 20)
    private RoomGoal roomGoal;

    // 커트라인 점수 (0~100)
    @Column(name = "cutline_score", nullable = false)
    private Integer cutlineScore;

    // 우대사항
    @Column(name = "preferred_conditions", length = 100)
    private String preferredConditions;

    // 채팅방 상태
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RoomStatus status;

    // Soft delete
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder
    public ChatRoom(Long jobMasterId, Long createdBy, String roomName,
                    Integer maxParticipants, RoomGoal roomGoal,
                    Integer cutlineScore, String preferredConditions) {
        this.jobMasterId = jobMasterId;
        this.createdBy = createdBy;
        this.roomName = roomName;
        this.maxParticipants = maxParticipants;
        this.roomGoal = roomGoal;
        this.cutlineScore = cutlineScore;
        this.preferredConditions = preferredConditions;
        this.status = RoomStatus.ACTIVE;  // 생성 시 기본값
    }

    // 채팅방 종료
    public void close() {
        this.status = RoomStatus.CLOSED;
    }
}