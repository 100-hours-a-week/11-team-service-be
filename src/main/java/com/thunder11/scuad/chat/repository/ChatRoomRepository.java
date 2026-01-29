package com.thunder11.scuad.chat.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.thunder11.scuad.chat.domain.ChatRoom;
import com.thunder11.scuad.chat.domain.type.RoomStatus;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    // 채팅방 ID로 조회 (삭제되지 않은 것만)
    @Query("SELECT cr FROM ChatRoom cr WHERE cr.chatRoomId = :chatRoomId AND cr.deletedAt IS NULL")
    Optional<ChatRoom> findByIdNotDeleted(@Param("chatRoomId") Long chatRoomId);

    // 공고별 채팅방 개수 조회 (ACTIVE 상태만)
    long countByJobMasterIdAndStatusAndDeletedAtIsNull(Long jobMasterId, RoomStatus status);

    // 방장이 특정 공고에 이미 방을 만들었는지 확인
    boolean existsByJobMasterIdAndCreatedByAndDeletedAtIsNull(Long jobMasterId, Long createdBy);

    // 공고별 채팅방 목록 조회 (커서 기반 페이징)
    @Query("SELECT cr FROM ChatRoom cr " +
            "WHERE cr.jobMasterId = :jobMasterId " +
            "AND cr.status = 'ACTIVE' " +
            "AND cr.deletedAt IS NULL " +
            "AND (:cursor IS NULL OR cr.chatRoomId < :cursor) " +
            "ORDER BY cr.chatRoomId DESC")
    List<ChatRoom> findByJobMasterIdWithCursor(
            @Param("jobMasterId") Long jobMasterId,
            @Param("cursor") Long cursor,
            Pageable pageable
    );
}