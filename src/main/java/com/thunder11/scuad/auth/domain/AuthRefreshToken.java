package com.thunder11.scuad.auth.domain;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;


// Access Token 재발급을 위한 Refresh Token 정보를 관리
// 로그아웃 시 무효화(revoke)하여 재발급 방지
@Entity
@Table(name = "auth_refresh_tokens")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuthRefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "token_id")
    private Long tokenId;

    // 토큰 소유자
    // LAZY 로딩: 토큰 검증 시 User 정보는 필요할 때만 로딩
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;


    // 보안을 위해 원본 토큰을 해시하여 저장
    // 유니크 제약으로 중복 방지
    @Column(name = "token_value", nullable = false, length = 64, unique = true)
    private String tokenValue;


    // 토큰 만료 시각
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;


    // 토큰 생성 시각
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 토큰 철회(무효화) 시각
    // null이면 유효한 토큰, 값이 있으면 무효화된 토큰
    // 로그아웃이나 보안 문제 시 설정
    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;


    @Builder
    public AuthRefreshToken(User user, String tokenValue, LocalDateTime expiresAt) {
        this.user = user;
        this.tokenValue = tokenValue;
        this.expiresAt = expiresAt;
        this.createdAt = LocalDateTime.now();
    }


     // 토큰 무효화, 로그아웃 시 호출하여 재발급 방지
    public void revoke() {
        this.revokedAt = LocalDateTime.now();
    }

    // 토큰 유효성 검증
    public boolean isValid() {
        return this.revokedAt == null && this.expiresAt.isAfter(LocalDateTime.now());
    }
}