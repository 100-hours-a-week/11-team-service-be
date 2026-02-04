package com.thunder11.scuad.chat.dto.response;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

// 메시지 목록 조회 응답 (페이징 포함)
@Getter
@Builder
public class ChatMessageListResponse {

    // 메시지 목록
    private List<ChatMessageResponse> messages;

    // 페이징 정보
    private PaginationResponse pagination;

    public static ChatMessageListResponse of(List<ChatMessageResponse> messages,
                                             PaginationResponse pagination) {
        return ChatMessageListResponse.builder()
                .messages(messages)
                .pagination(pagination)
                .build();
    }
}