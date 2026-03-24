package com.thunder11.scuad.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 회원 탈퇴 요청 DTO
// DELETE /api/v1/users/me
// reason 필수: 빈 값 수집을 방지하고 서비스 개선에 의미 있는 데이터만 저장
@Getter
@NoArgsConstructor
public class UserWithdrawalRequest {

    // 탈퇴 사유 (필수)
    // user_withdrawals.reason TEXT 기준 최대 1000자 제한
    @NotBlank(message = "탈퇴 사유는 필수입니다.")
    @Size(max = 1000, message = "탈퇴 사유는 1000자 이하여야 합니다.")
    private String reason;
}
