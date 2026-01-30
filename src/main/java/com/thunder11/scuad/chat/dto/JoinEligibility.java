package com.thunder11.scuad.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;


// 채팅방 입장 가능 여부 판단 결과
@Getter
@AllArgsConstructor
public class JoinEligibility {
    // 입장 가능 여부
    private final boolean canJoin;

    // 입장 상태 코드
    // ALREADY_JOINED, FULL, KICKED, NO_APPLICATION, NO_RESUME,
    // NO_PORTFOLIO, NO_SCORE, CUTLINE_NOT_MET,
    // ALREADY_JOINED_OTHER, AVAILABLE
    private final String status;

    // 입장 불가 상태 생성
    public static JoinEligibility unavailable(String status) {
        return new JoinEligibility(false, status);
    }

    // 입장 가능 상태 생성
    public static JoinEligibility available() {
        return new JoinEligibility(true, "AVAILABLE");
    }
}