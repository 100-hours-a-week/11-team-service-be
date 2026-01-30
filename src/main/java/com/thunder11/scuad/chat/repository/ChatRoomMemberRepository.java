package com.thunder11.scuad.chat.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.thunder11.scuad.chat.domain.ChatRoomMember;

public interface ChatRoomMemberRepository extends JpaRepository<ChatRoomMember, Long> {

    // 채팅방 내 특정 사용자의 멤버십 조회 (강퇴되지 않은 멤버만)
    Optional<ChatRoomMember> findByChatRoomIdAndUserIdAndKickedAtIsNull(Long chatRoomId, Long userId);

    // 채팅방의 현재 인원 수 (강퇴되지 않은 멤버만)
    long countByChatRoomIdAndKickedAtIsNull(Long chatRoomId);

    // 사용자가 특정 공고의 채팅방에 이미 참여 중인지 확인 (job_application_id로 확인)
    @Query("SELECT crm FROM ChatRoomMember crm " +
            "WHERE crm.jobApplicationId = :jobApplicationId " +
            "AND crm.kickedAt IS NULL")
    Optional<ChatRoomMember> findByJobApplicationIdAndNotKicked(@Param("jobApplicationId") Long jobApplicationId);

    // 채팅방 멤버 ID로 조회 (강퇴되지 않은 멤버만)
    Optional<ChatRoomMember> findByChatRoomMemberIdAndKickedAtIsNull(Long chatRoomMemberId);

    // 특정 채팅방에서 사용자가 방장인지 확인
    @Query("SELECT CASE WHEN COUNT(crm) > 0 THEN true ELSE false END " +
            "FROM ChatRoomMember crm " +
            "WHERE crm.chatRoomId = :chatRoomId " +
            "AND crm.userId = :userId " +
            "AND crm.role = 'HOST' " +
            "AND crm.kickedAt IS NULL")
    boolean isHostOfRoom(@Param("chatRoomId") Long chatRoomId, @Param("userId") Long userId);

    // 강퇴 여부 확인 (kicked_at이 있는 레코드 존재 여부)
    @Query("SELECT CASE WHEN COUNT(crm) > 0 THEN true ELSE false END " +
            "FROM ChatRoomMember crm " +
            "WHERE crm.chatRoomId = :chatRoomId " +
            "AND crm.userId = :userId " +
            "AND crm.kickedAt IS NOT NULL")
    boolean existsKickedMember(@Param("chatRoomId") Long chatRoomId, @Param("userId") Long userId);
}