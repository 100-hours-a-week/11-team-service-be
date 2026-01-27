package com.thunder11.scuad.auth.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

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
}