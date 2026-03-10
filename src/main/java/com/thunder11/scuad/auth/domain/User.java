package com.thunder11.scuad.auth.domain;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

import com.thunder11.scuad.common.entity.BaseTimeEntity;

// 서비스의 핵심 사용자 정보 관리
// OAuth 계정 정보는 UserOAuthAccount에서 별도 관리해 분리
@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    // 프로필 이미지 파일 ID
    // 추후 FileObject 엔티티와 연관 매핑
    @Column(name = "profile_image_file_id")
    private Long profileImageFileId;  // 일단 Long으로 (나중에 FileObject 연관관계)

    // 사용자 역할 String으로 저장
    @Enumerated(EnumType.STRING)  // Enum을 문자열로 저장
    @Column(name = "role", nullable = false, length = 20)
    private Role role;

    // 사용자 닉네임 (중복 불가)
    @Column(name = "nickname", nullable = false, length = 30, unique = true)
    private String nickname;

    // 계정 상태 String으로 저장
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private UserStatus status;

    // 탈퇴 시각
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // 생성자 (빌더 패턴도 가능)
    @Builder
    public User(String nickname, Role role, UserStatus status) {
        this.nickname = nickname;
        this.role = role;
        this.status = status;
    }

    // 닉네임 변경
    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

    // 프로필 이미지 파일 ID 변경
    // null 허용: 이미지 제거 시 null 전달
    // 파일 존재 여부 검증은 서비스 레이어에서 수행
    public void updateProfileImageFileId(Long profileImageFileId) {
        this.profileImageFileId = profileImageFileId;
    }

    // 탈퇴 처리: 상태 변경 + deleted_at 설정 + 닉네임 패턴 변경
    // 닉네임을 deleted_{userId}_{원본닉네임} 패턴으로 변경하여 UNIQUE 제약을 해제
    // 이렇게 해야 동일 카카오 계정으로 재가입 시 같은 닉네임 사용이 가능
    // VARCHAR(30) 제한 초과 시 원본 닉네임을 truncate 처리
    public void applyWithdrawalNicknamePattern() {
        String prefix = "deleted_" + this.userId + "_";
        int maxLength = 30;
        int remainingLength = maxLength - prefix.length();

        String truncatedOriginal = this.nickname.length() > remainingLength
                ? this.nickname.substring(0, Math.max(remainingLength, 0))
                : this.nickname;

        this.nickname = prefix + truncatedOriginal;
        this.status = UserStatus.WITHDRAWN;
        this.deletedAt = LocalDateTime.now();
    }
}