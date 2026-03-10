package com.thunder11.scuad.user.dto;

import com.thunder11.scuad.auth.domain.User;

import lombok.Builder;
import lombok.Getter;

// 현재 로그인한 사용자 정보 응답 DTO
// GET /api/v1/users/me 응답용
@Getter
@Builder
public class UserResponse {

    private Long userId;
    private String nickname;
    private String email;               // OAuth 계정 이메일 (null 허용, read-only)
    private Long profileImageFileId;    // 파일 식별자 (null 허용)
    // presigned URL 포함: 클라이언트가 추가 API 호출 없이 이미지를 즉시 렌더링하기 위함
    private String profileImageUrl;     // S3 presigned URL (null 허용, 이미지 없으면 null)
    private String role;
    private String status;

    // User 엔티티, 이메일, presigned URL로 UserResponse 생성
    public static UserResponse of(User user, String email, String profileImageUrl) {
        return UserResponse.builder()
                .userId(user.getUserId())
                .nickname(user.getNickname())
                .email(email)
                .profileImageFileId(user.getProfileImageFileId())
                .profileImageUrl(profileImageUrl)
                .role(user.getRole().name())
                .status(user.getStatus().name())
                .build();
    }
}