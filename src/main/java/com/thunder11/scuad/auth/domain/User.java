package com.thunder11.scuad.auth.domain;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

import com.thunder11.scuad.common.entity.BaseTimeEntity;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "profile_image_file_id")
    private Long profileImageFileId;  // 일단 Long으로 (나중에 FileObject 연관관계)

    @Enumerated(EnumType.STRING)  // Enum을 문자열로 저장
    @Column(name = "role", nullable = false, length = 20)
    private Role role;

    @Column(name = "nickname", nullable = false, length = 30, unique = true)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private UserStatus status;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // 생성자 (빌더 패턴도 가능)
    @Builder
    public User(String nickname, Role role, UserStatus status) {
        this.nickname = nickname;
        this.role = role;
        this.status = status;
    }

    // 비즈니스 메서드
    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

    public void withdraw() {
        this.status = UserStatus.WITHDRAWN;
        this.deletedAt = LocalDateTime.now();
    }
}