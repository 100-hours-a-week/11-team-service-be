package com.thunder11.scuad.chat.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

// 내 채팅방 목록 응답 (페이징 포함)
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MyChatRoomListResponse {

    // 채팅방 목록
    private List<MyChatRoomResponse> chatRooms;

    // 페이징 정보
    private PaginationResponse pagination;
}