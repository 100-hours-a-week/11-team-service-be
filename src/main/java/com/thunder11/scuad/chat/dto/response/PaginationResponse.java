package com.thunder11.scuad.chat.dto.response;

import lombok.Builder;
import lombok.Getter;

// 커서 기반 페이징 정보
@Getter
@Builder
public class PaginationResponse {

    // 다음 커서 (다음 페이지의 시작 ID)
    private Long nextCursor;

    // 다음 페이지 존재 여부
    private Boolean hasNext;

    // 현재 페이지 사이즈
    private Integer size;

    public static PaginationResponse of(Long nextCursor, Boolean hasNext, Integer size) {
        return PaginationResponse.builder()
                .nextCursor(nextCursor)
                .hasNext(hasNext)
                .size(size)
                .build();
    }
}