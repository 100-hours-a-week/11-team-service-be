package com.thunder11.scuad.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 카카오 사용자 정보 조회 API 응답 DTO
// GET https://kapi.kakao.com/v2/user/me 의 응답을 매핑
@Getter
@NoArgsConstructor
public class KakaoUserInfoResponse {

    // 카카오 회원번호
    private Long id;

    // 카카오 계정 정보
    @JsonProperty("kakao_account")
    private KakaoAccount kakaoAccount;

    // 카카오 계정 정보
    @Getter
    @NoArgsConstructor
    public static class KakaoAccount {
        // 카카오 계정 이메일
        private String email;

        // 카카오 프로필 정보
        private Profile profile;
    }

    // 카카오 프로필 정보
    @Getter
    @NoArgsConstructor
    public static class Profile {
        // 카카오 닉네임
        private String nickname;

        // 카카오 프로필 이미지 URL
        @JsonProperty("profile_image_url")
        private String profileImageUrl;
    }
}