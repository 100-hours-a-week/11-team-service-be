package com.thunder11.scuad.chat.domain;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

import com.thunder11.scuad.chat.domain.type.MessageType;

// 채팅방 메시지
@Entity
@Table(name = "chat_messages")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "message_id")
    private Long messageId;

    // 채팅방 (chat_rooms)
    @Column(name = "chat_room_id", nullable = false)
    private Long chatRoomId;

    // 발신자 (users) - SYSTEM 메시지는 NULL
    @Column(name = "sender_id")
    private Long senderId;

    // 첨부파일 (file_objects)
    @Column(name = "file_id")
    private Long fileId;

    // 메시지 타입
    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false, length = 20)
    private MessageType messageType;

    // 텍스트 내용 (파일만 전송 시 NULL 가능)
    @Column(name = "content", length = 1000)
    private String content;

    // 전송 시각
    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;

    // Soft delete
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder
    public ChatMessage(Long chatRoomId, Long senderId, Long fileId,
                       MessageType messageType, String content) {
        this.chatRoomId = chatRoomId;
        this.senderId = senderId;
        this.fileId = fileId;
        this.messageType = messageType;
        this.content = content;
        this.sentAt = LocalDateTime.now();
    }

    // 시스템 메시지 생성 팩토리 메서드
    public static ChatMessage createSystemMessage(Long chatRoomId, String content) {
        return ChatMessage.builder()
                .chatRoomId(chatRoomId)
                .senderId(null)  // 시스템 메시지는 sender 없음
                .messageType(MessageType.SYSTEM)
                .content(content)
                .build();
    }
}