package com.thunder11.scuad.chat.repository;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.thunder11.scuad.chat.domain.ChatRoom;
import com.thunder11.scuad.chat.domain.type.RoomStatus;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

        // 채팅방 ID로 조회 (삭제되지 않은 것만)
        @Query("SELECT cr FROM ChatRoom cr WHERE cr.chatRoomId = :chatRoomId AND cr.deletedAt IS NULL")
        Optional<ChatRoom> findByIdNotDeleted(@Param("chatRoomId") Long chatRoomId);

        // 채팅방 ID로 조회 + 비관적 락 (SELECT FOR UPDATE)
        // 이유: 동시 입장 요청 시 정원 체크(count)와 저장(save) 사이의 gap에서 race condition 발생
        //       첫 번째 요청이 이 락을 잡는 순간 두 번째 요청은 락 해제까지 대기하므로
        //       정원 체크가 항상 최신 값을 기준으로 직렬화됨
        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("SELECT cr FROM ChatRoom cr WHERE cr.chatRoomId = :chatRoomId AND cr.deletedAt IS NULL")
        Optional<ChatRoom> findByIdWithLock(@Param("chatRoomId") Long chatRoomId);

        // 공고별 채팅방 개수 조회 (ACTIVE 상태만)
        long countByJobMasterIdAndStatusAndDeletedAtIsNull(Long jobMasterId, RoomStatus status);

        // 방장이 특정 공고에 이미 방을 만들었는지 확인
        boolean existsByJobMasterIdAndCreatedByAndDeletedAtIsNullAndStatus(
                Long jobMasterId, Long createdBy, RoomStatus status);

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
                        Pageable pageable);

        @Query("SELECT cr.jobMasterId, COUNT(cr) FROM ChatRoom cr " +
                        "WHERE cr.jobMasterId IN :jobMasterIds " +
                        "AND cr.status = 'ACTIVE' " +
                        "AND cr.deletedAt IS NULL " +
                        "GROUP BY cr.jobMasterId")
        List<Object[]> countActiveRoomsByJobMasterIds(@Param("jobMasterIds") List<Long> jobMasterIds);
}