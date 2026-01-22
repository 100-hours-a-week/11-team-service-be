package com.thunder11.scuad.auth.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.thunder11.scuad.auth.domain.AuthRefreshToken;
import com.thunder11.scuad.auth.domain.User;


// 토큰 저장, 조회, 무효화 기능 제공
public interface AuthRefreshTokenRepository extends JpaRepository<AuthRefreshToken, Long> {

    //토큰 값으로 Refresh Token 조회
    Optional<AuthRefreshToken> findByTokenValue(String tokenValue);

     // 만료되지 않고 철회되지 않은 유효한 토큰만 조회
    Optional<AuthRefreshToken> findByTokenValueAndRevokedAtIsNullAndExpiresAtAfter(
            String tokenValue,
            LocalDateTime now
    );

    // 사용자의 모든 Refresh Token 조회
    // 전체 기기 로그아웃 시 사용
    List<AuthRefreshToken> findByUser(User user);

    // 만료된 토큰 삭제용 조회
    // 배치 작업에서 사용
    List<AuthRefreshToken> findByExpiresAtBefore(LocalDateTime now);
}