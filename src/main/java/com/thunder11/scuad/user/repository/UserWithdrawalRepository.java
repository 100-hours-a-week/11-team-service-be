package com.thunder11.scuad.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.thunder11.scuad.user.domain.UserWithdrawal;

// 회원 탈퇴 사유 리포지토리
// user_withdrawals 테이블 접근
public interface UserWithdrawalRepository extends JpaRepository<UserWithdrawal, Long> {

    // 사용자 ID로 탈퇴 기록 존재 여부 확인 (중복 탈퇴 방지용)
    boolean existsByUser_UserId(Long userId);
}