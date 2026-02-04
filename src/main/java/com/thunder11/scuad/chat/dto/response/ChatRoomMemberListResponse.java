package com.thunder11.scuad.chat.dto.response;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

/**
 * 채팅방 멤버 목록 응답 DTO
 * GET /api/v1/chat-rooms/{chatRoomId}/members 응답에 사용
 */
@Getter
@Builder
public class ChatRoomMemberListResponse {

    private List<ChatRoomMemberResponse> members;
    private int totalCount;

    public static ChatRoomMemberListResponse of(List<ChatRoomMemberResponse> members) {
        return ChatRoomMemberListResponse.builder()
                .members(members)
                .totalCount(members.size())
                .build();
    }
}
