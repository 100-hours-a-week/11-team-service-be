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

    // 채팅방 멤버 목록 조회 (활성 멤버만, 방장 우선 정렬)
    @Query("SELECT crm FROM ChatRoomMember crm " +
            "WHERE crm.chatRoomId = :chatRoomId " +
            "AND crm.kickedAt IS NULL " +
            "ORDER BY CASE WHEN crm.role = 'HOST' THEN 0 ELSE 1 END, crm.joinedAt ASC")
    java.util.List<ChatRoomMember> findAllActiveMembersByChatRoomId(@Param("chatRoomId") Long chatRoomId);

    // 내가 참여 중인 채팅방 목록 조회 (커서 기반 페이징, 최신 참여 순)
    // 테이블 정의서의 idx_chat_members_user 인덱스 활용
    //       (user_id, kicked_at, joined_at DESC)로 최적화된 쿼리
    // 사용자가 참여 중인 채팅방을 최신 참여 순으로 보여주어 활동성 높은 방 우선 노출
    @Query("SELECT crm FROM ChatRoomMember crm " +
            "WHERE crm.userId = :userId " +
            "AND crm.kickedAt IS NULL " +
            "AND (:cursor IS NULL OR crm.joinedAt < " +
            "    (SELECT crm2.joinedAt FROM ChatRoomMember crm2 WHERE crm2.chatRoomMemberId = :cursor)) " +
            "ORDER BY crm.joinedAt DESC")
    java.util.List<ChatRoomMember> findMyChatRooms(
            @Param("userId") Long userId,
            @Param("cursor") Long cursor,
            org.springframework.data.domain.Pageable pageable
    );
}