package com.thunder11.scuad.auth.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.thunder11.scuad.auth.domain.User;

public interface UserRepository extends JpaRepository<User, Long> {

    // 닉네임으로 사용자 찾기
    Optional<User> findByNickname(String nickname);

    // 닉네임 중복 체크
    boolean existsByNickname(String nickname);
}