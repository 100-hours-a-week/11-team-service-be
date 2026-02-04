package com.thunder11.scuad.user.dto;

import com.thunder11.scuad.auth.domain.Role;
import com.thunder11.scuad.auth.domain.User;
import com.thunder11.scuad.auth.domain.UserStatus;

import lombok.Builder;
import lombok.Getter;

// 현재 로그인한 사용자 정보 응답 DTO
// GET /api/v1/users/me 응답용
@Getter
@Builder
public class UserResponse {

    private Long userId;
    private String nickname;
    private String email;  // OAuth 계정의 이메일 (null 가능)
    private Long profileImageFileId;
    private String role;
    private String status;


    // User 엔티티와 이메일로 UserResponse 생성
    public static UserResponse of(User user, String email) {
        return UserResponse.builder()
                .userId(user.getUserId())
                .nickname(user.getNickname())
                .email(email)
                .profileImageFileId(user.getProfileImageFileId())
                .role(user.getRole().name())
                .status(user.getStatus().name())
                .build();
    }
}
