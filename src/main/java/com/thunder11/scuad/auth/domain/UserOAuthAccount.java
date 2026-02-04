package com.thunder11.scuad.auth.domain;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

import com.thunder11.scuad.common.entity.BaseTimeEntity;

// 사용자 OAuth 계정 연동 정보
// 한 사용자가 여러 OAuth 제공자 계정 연동 가능 (지금은 kakao)
@Entity
@Table(name = "user_oauth_accounts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserOAuthAccount extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "oauth_account_id")
    private Long oauthAccountId;

    // 연동된 사용자
    // LAZY 로딩: OAuth 계정 조회 시 User는 필요할 때만 로딩
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 우리 서비스에서 사용하는 대표 이메일
    @Column(name = "email", nullable = false, length = 100)
    private String email;

    // OAuth 제공자
    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 20)
    private OAuthProvider provider;

    // OAuth 제공자 고유 ID
    @Column(name = "provider_user_id", nullable = false, length = 100)
    private String providerUserId;

    // OAuth 제공자가 제공한 원본 이메일
    @Column(name = "provider_email", length = 255)
    private String providerEmail;

    // OAuth 연동 시각
    @Column(name = "connected_at", nullable = false)
    private LocalDateTime connectedAt;

    // 연동 해제 시각
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder
    public UserOAuthAccount(User user, String email, OAuthProvider provider,
                            String providerUserId, String providerEmail,
                            LocalDateTime connectedAt) {
        this.user = user;
        this.email = email;
        this.provider = provider;
        this.providerUserId = providerUserId;
        this.providerEmail = providerEmail;
        this.connectedAt = connectedAt;
    }
}