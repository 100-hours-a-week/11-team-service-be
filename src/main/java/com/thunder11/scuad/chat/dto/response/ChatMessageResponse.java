package com.thunder11.scuad.chat.dto.response;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Getter;

import com.thunder11.scuad.chat.domain.ChatMessage;
import com.thunder11.scuad.chat.domain.type.MessageType;

// 메시지 조회 응답
@Getter
@Builder
public class ChatMessageResponse {

    private Long messageId;
    private Long senderId;
    private String senderNickname;
    private MessageType messageType;
    private String content;

    // 파일 정보 (FILE 타입일 때만)
    private FileInfo file;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    // 파일 정보 내부 클래스
    @Getter
    @Builder
    public static class FileInfo {
        private Long fileId;
        private String fileName;
        private String fileUrl;
    }

    // 엔티티로부터 생성하는 팩토리 메서드
    public static ChatMessageResponse from(ChatMessage message, String senderNickname) {
        return ChatMessageResponse.builder()
                .messageId(message.getMessageId())
                .senderId(message.getSenderId())
                .senderNickname(senderNickname)
                .messageType(message.getMessageType())
                .content(message.getContent())
                .createdAt(message.getSentAt())
                .build();
    }
}