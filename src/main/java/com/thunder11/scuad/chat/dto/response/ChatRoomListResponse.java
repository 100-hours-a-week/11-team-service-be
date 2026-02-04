package com.thunder11.scuad.chat.dto.response;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

// 채팅방 목록 조회 응답 (페이징 포함)
@Getter
@Builder
public class ChatRoomListResponse {

    // 내 점수
    private Integer myScore;

    // 채팅방 목록
    private List<ChatRoomSummaryResponse> chatRooms;

    // 페이징 정보
    private PaginationResponse pagination;

    public static ChatRoomListResponse of(Integer myScore,
                                          List<ChatRoomSummaryResponse> chatRooms,
                                          PaginationResponse pagination) {
        return ChatRoomListResponse.builder()
                .myScore(myScore)
                .chatRooms(chatRooms)
                .pagination(pagination)
                .build();
    }
}