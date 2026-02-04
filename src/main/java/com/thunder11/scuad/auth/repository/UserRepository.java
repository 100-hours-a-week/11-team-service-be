package com.thunder11.scuad.auth.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.thunder11.scuad.auth.domain.User;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

    // 닉네임으로 사용자 찾기
    Optional<User> findByNickname(String nickname);

    // 닉네임 중복 체크
    boolean existsByNickname(String nickname);

    // userId로 닉네임 조회 (채팅방 방장 표시용)
    @Query("SELECT u.nickname FROM User u WHERE u.userId = :userId")
    Optional<String> findNicknameByUserId(@Param("userId") Long userId);

    // 여러 userId의 닉네임 일괄 조회 (메시지 발신자 표시용)
    @Query("SELECT u.userId, u.nickname FROM User u WHERE u.userId IN :userIds")
    List<Object[]> findNicknamesByUserIds(@Param("userIds") List<Long> userIds);
}