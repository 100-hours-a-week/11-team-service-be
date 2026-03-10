package com.thunder11.scuad.user.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.thunder11.scuad.auth.domain.User;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 회원 탈퇴 사유 기록 엔티티
// user_withdrawals 테이블 매핑
// user_id UNIQUE 제약으로 유저당 1회 탈퇴만 기록 (DB 레벨 보장)
// 서비스 개선 및 탈퇴 통계 분석 목적으로 사용
@Entity
@Table(name = "user_withdrawals")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class UserWithdrawal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "withdrawal_id")
    private Long withdrawalId;

    // 탈퇴한 사용자 (UNIQUE 제약으로 동일 사용자 중복 기록 방지)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    // 탈퇴 사유 (필수, 서비스 개선 목적)
    @Column(name = "reason", nullable = false, columnDefinition = "TEXT")
    private String reason;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public UserWithdrawal(User user, String reason) {
        this.user = user;
        this.reason = reason;
    }
}