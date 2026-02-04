package com.thunder11.scuad.chat.dto.response;

import java.time.LocalDateTime;

import com.thunder11.scuad.chat.domain.ChatRoomMember;
import com.thunder11.scuad.chat.domain.type.MemberRole;

import lombok.Builder;
import lombok.Getter;

/**
 * 채팅방 멤버 정보 응답 DTO
 * GET /api/v1/chat-rooms/{chatRoomId}/members 응답에 사용
 */
@Getter
@Builder
public class ChatRoomMemberResponse {

    private Long chatRoomMemberId;
    private Long userId;
    private String nickname;  // User 도메인에서 조회
    private String role;  // HOST, MEMBER
    private LocalDateTime joinedAt;

    /**
     * ChatRoomMember 엔티티와 닉네임으로 응답 DTO 생성
     */
    public static ChatRoomMemberResponse of(ChatRoomMember member, String nickname) {
        return ChatRoomMemberResponse.builder()
                .chatRoomMemberId(member.getChatRoomMemberId())
                .userId(member.getUserId())
                .nickname(nickname)
                .role(member.getRole().name())
                .joinedAt(member.getJoinedAt())
                .build();
    }
}
