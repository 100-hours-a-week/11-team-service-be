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
    private String nickname;
    private String profileImageUrl;  // S3 presigned URL (null 허용, 이미지 없으면 null)
    private String role;
    private LocalDateTime joinedAt;

    /**
     * ChatRoomMember 엔티티, 닉네임, 프로필 이미지 URL로 응답 DTO 생성
     *
     * profileImageUrl을 추가한 이유:
     *   유저가 마이페이지에서 프로필 이미지를 변경해도 채팅방 멤버 목록에 반영되지 않는 문제가 있었음.
     *   기존에는 닉네임만 조회하고 profileImageUrl이 응답에 포함되지 않았기 때문.
     *   User 엔티티의 profileImageFileId를 기반으로 presigned URL을 생성해서 응답에 포함하도록 수정.
     */
    public static ChatRoomMemberResponse of(ChatRoomMember member, String nickname, String profileImageUrl) {
        return ChatRoomMemberResponse.builder()
                .chatRoomMemberId(member.getChatRoomMemberId())
                .userId(member.getUserId())
                .nickname(nickname)
                .profileImageUrl(profileImageUrl)
                .role(member.getRole().name())
                .joinedAt(member.getJoinedAt())
                .build();
    }
}
