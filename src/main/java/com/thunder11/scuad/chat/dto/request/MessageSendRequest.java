package com.thunder11.scuad.chat.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import com.thunder11.scuad.chat.domain.type.MessageType;

// 메시지 전송 요청
@Getter
@NoArgsConstructor
public class MessageSendRequest {

    @NotNull(message = "메시지 타입은 필수입니다")
    private MessageType messageType;

    @Size(max = 1000, message = "메시지 내용은 1000자 이하여야 합니다")
    private String content;

    // 파일 ID (FILE 타입일 때 사용)
    private Long fileId;
}