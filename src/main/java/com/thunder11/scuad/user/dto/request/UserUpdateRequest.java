package com.thunder11.scuad.user.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 회원정보 수정 요청 DTO
// PATCH /api/v1/users/me
// 모든 필드는 선택적(Optional): 요청에 포함된 필드만 수정하는 PATCH 시맨틱
// email은 OAuth 제공 정보이므로 수정 대상에서 제외
@Getter
@NoArgsConstructor
public class UserUpdateRequest {

    // 변경할 닉네임 (선택)
    // users.nickname VARCHAR(30) 기준으로 길이 제한
    @Size(min = 1, max = 30, message = "닉네임은 1자 이상 30자 이하여야 합니다.")
    private String nickname;

    // 변경할 프로필 이미지 파일 ID (선택)
    // file_objects 존재 여부는 서비스 레이어에서 검증
    private Long profileImageFileId;

    // 닉네임 변경 요청 여부: null이면 변경 요청 없음
    public boolean hasNicknameUpdate() {
        return nickname != null;
    }

    // 프로필 이미지 변경 요청 여부: null이면 변경 요청 없음
    public boolean hasProfileImageUpdate() {
        return profileImageFileId != null;
    }
}