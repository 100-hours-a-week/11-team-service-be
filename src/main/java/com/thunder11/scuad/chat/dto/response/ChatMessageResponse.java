package com.thunder11.scuad.chat.dto.response;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import com.thunder11.scuad.chat.domain.ChatMessage;
import com.thunder11.scuad.chat.domain.type.MessageType;

// 메시지 조회 응답
// @NoArgsConstructor, @AllArgsConstructor 추가 근거:
//   Redis Pub/Sub 도입으로 이 DTO가 Redis → JSON → DTO 역직렬화 경로를 거치게 됨
//   Jackson은 역직렬화 시 기본 생성자(@NoArgsConstructor)가 필요하고
//   @Builder는 전체 인자 생성자(@AllArgsConstructor)와 함께 써야 정상 동작함
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
    // @NoArgsConstructor, @AllArgsConstructor: 외부 클래스와 동일한 이유로 Jackson 역직렬화 지원
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FileInfo {
        private Long fileId;
        private String fileName;
        private String fileUrl;
        private Long fileSize;
        private String contentType;
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