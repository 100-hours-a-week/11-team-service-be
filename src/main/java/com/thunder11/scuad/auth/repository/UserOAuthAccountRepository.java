package com.thunder11.scuad.auth.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.thunder11.scuad.auth.domain.OAuthProvider;
import com.thunder11.scuad.auth.domain.UserOAuthAccount;

public interface UserOAuthAccountRepository extends JpaRepository<UserOAuthAccount, Long> {

    // 카카오 로그인 시 이미 가입된 계정인지 확인
    Optional<UserOAuthAccount> findByProviderAndProviderUserId(
            OAuthProvider provider,
            String providerUserId
    );

    // 중복 가입 방지용
    boolean existsByProviderAndProviderUserId(
            OAuthProvider provider,
            String providerUserId
    );

    // userId로 이메일 조회 (현재 사용자 정보 조회용)
    // 카카오 OAuth 기준으로 email 조회
    @Query("SELECT uoa.email FROM UserOAuthAccount uoa WHERE uoa.user.userId = :userId AND uoa.provider = 'KAKAO'")
    Optional<String> findEmailByUserId(@Param("userId") Long userId);
}