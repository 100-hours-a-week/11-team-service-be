package com.thunder11.scuad.chat.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.thunder11.scuad.chat.domain.ChatMessage;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    // 채팅방의 최신 메시지 조회 (커서 기반 페이징)
    @Query("SELECT cm FROM ChatMessage cm " +
            "WHERE cm.chatRoomId = :chatRoomId " +
            "AND cm.deletedAt IS NULL " +
            "AND (:cursor IS NULL OR cm.messageId < :cursor) " +
            "ORDER BY cm.messageId DESC")
    List<ChatMessage> findMessagesByChatRoomIdWithCursor(
            @Param("chatRoomId") Long chatRoomId,
            @Param("cursor") Long cursor,
            Pageable pageable
    );

    // 폴링을 위한 최신 메시지 조회 (특정 messageId 이후의 메시지)
    @Query("SELECT cm FROM ChatMessage cm " +
            "WHERE cm.chatRoomId = :chatRoomId " +
            "AND cm.deletedAt IS NULL " +
            "AND cm.messageId > :afterMessageId " +
            "ORDER BY cm.messageId ASC")
    List<ChatMessage> findNewMessagesSince(
            @Param("chatRoomId") Long chatRoomId,
            @Param("afterMessageId") Long afterMessageId
    );
}