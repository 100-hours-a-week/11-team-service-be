package com.thunder11.scuad.chat.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.thunder11.scuad.chat.domain.ChatMessage;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    // 채팅방의 최신 메시지 조회 (커서 기반 페이징)
    // 수정 이유: 채팅 메시지는 오래된 것부터 최신 순으로 정렬되어야 프론트엔드에서
    //           올바른 순서로 렌더링되고, 폴링 시 cursor(마지막 메시지 ID) 추적이 정확함
    @Query("SELECT cm FROM ChatMessage cm " +
            "WHERE cm.chatRoomId = :chatRoomId " +
            "AND cm.deletedAt IS NULL " +
            "AND (:cursor IS NULL OR cm.messageId > :cursor) " +
            "ORDER BY cm.messageId ASC")
    List<ChatMessage> findMessagesByChatRoomIdWithCursor(
            @Param("chatRoomId") Long chatRoomId,
            @Param("cursor") Long cursor,
            Pageable pageable
    );

    Optional<ChatMessage> findTopByChatRoomIdOrderBySentAtDesc(Long chatRoomId);

    // 채팅방 ID 목록별 마지막 메시지 일괄 조회 (greatest-n-per-group, IN + 서브쿼리)
    // 사용 목적: getMyChatRooms 에서 stream 내 findTopBy...() N번 → 1번으로 대체
    // 설계 근거: 채팅방마다 가장 최근 메시지 1건만 가져오는 greatest-n-per-group 문제를
    //           JPQL 상관 서브쿼리(MAX sentAt 매칭)로 표현하여 QueryDSL/Native 의존 없이 해결
    //           sentAt 동일 메시지가 여러 건인 엣지 케이스는 미리보기 목적상 허용 범위로 판단
    @Query("""
            SELECT cm FROM ChatMessage cm
            WHERE cm.chatRoomId IN :chatRoomIds
              AND cm.sentAt = (
                  SELECT MAX(cm2.sentAt) FROM ChatMessage cm2
                  WHERE cm2.chatRoomId = cm.chatRoomId
              )
            """)
    List<ChatMessage> findLastMessagesByChatRoomIds(@Param("chatRoomIds") List<Long> chatRoomIds);
}