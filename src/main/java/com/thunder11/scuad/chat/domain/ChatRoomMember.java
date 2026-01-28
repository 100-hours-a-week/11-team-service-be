package com.thunder11.scuad.chat.domain;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

import com.thunder11.scuad.chat.domain.type.MemberRole;

// 채팅방 멤버십 관리
@Entity
@Table(name = "chat_room_members")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoomMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chat_room_member_id")
    private Long chatRoomMemberId;

    // 채팅방 (chat_rooms)
    @Column(name = "chat_room_id", nullable = false)
    private Long chatRoomId;

    // 사용자 (users)
    @Column(name = "user_id", nullable = false)
    private Long userId;

    // 지원 근거 (job_applications) - UNIQUE 제약
    @Column(name = "job_application_id", nullable = false, unique = true)
    private Long jobApplicationId;

    // 멤버 역할
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private MemberRole role;

    // 입장 시각
    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    // 강퇴 시각 (NULL이면 미강퇴)
    @Column(name = "kicked_at")
    private LocalDateTime kickedAt;

    @Builder
    public ChatRoomMember(Long chatRoomId, Long userId, Long jobApplicationId, MemberRole role) {
        this.chatRoomId = chatRoomId;
        this.userId = userId;
        this.jobApplicationId = jobApplicationId;
        this.role = role;
        this.joinedAt = LocalDateTime.now();
    }

    // 강퇴 처리
    public void kick() {
        this.kickedAt = LocalDateTime.now();
    }

    // 강퇴 여부 확인
    public boolean isKicked() {
        return this.kickedAt != null;
    }
}