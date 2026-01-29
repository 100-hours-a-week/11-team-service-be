package com.thunder11.scuad.chat.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.thunder11.scuad.chat.domain.ChatMessage;
import com.thunder11.scuad.chat.dto.response.ChatMessageListResponse;
import com.thunder11.scuad.chat.dto.response.ChatMessageResponse;
import com.thunder11.scuad.chat.dto.response.PaginationResponse;
import com.thunder11.scuad.chat.repository.ChatMessageRepository;
import com.thunder11.scuad.chat.repository.ChatRoomMemberRepository;
import com.thunder11.scuad.chat.repository.ChatRoomRepository;
import com.thunder11.scuad.common.exception.ApiException;
import com.thunder11.scuad.common.exception.ErrorCode;

// 채팅 메시지 관련 비즈니스 로직 처리
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;

    // 채팅 메시지 목록 조회 (커서 기반 페이징 + 폴링)
    public ChatMessageListResponse getMessages(
            Long chatRoomId,
            Long userId,
            Long cursor,
            Long since,
            int size
    ) {
        log.info("메시지 목록 조회 시작: chatRoomId={}, userId={}, cursor={}, since={}, size={}",
                chatRoomId, userId, cursor, since, size);

        // 1. 채팅방 존재 확인
        chatRoomRepository.findByIdNotDeleted(chatRoomId)
                .orElseThrow(() -> new ApiException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        // 2. 멤버십 확인 (참여자만 메시지 조회 가능)
        boolean isMember = chatRoomMemberRepository
                .findByChatRoomIdAndUserIdAndKickedAtIsNull(chatRoomId, userId)
                .isPresent();

        if (!isMember) {
            log.warn("채팅방 멤버가 아닌 사용자의 메시지 조회 시도: chatRoomId={}, userId={}", chatRoomId, userId);
            throw new ApiException(ErrorCode.CHAT_ROOM_ACCESS_DENIED);
        }

        List<ChatMessage> messages;

        // 3. 폴링 요청인지 과거 메시지 로드인지 구분
        if (since != null) {
            // 폴링: since 이후의 최신 메시지 조회
            messages = chatMessageRepository.findNewMessagesSince(chatRoomId, since);
            log.info("폴링 메시지 조회 완료: {}개", messages.size());
        } else {
            // 과거 메시지 로드: 커서 기반 페이징
            messages = chatMessageRepository.findMessagesByChatRoomIdWithCursor(
                    chatRoomId,
                    cursor,
                    PageRequest.of(0, size + 1)
            );
            log.info("과거 메시지 조회 완료: {}개", messages.size());
        }

        // 4. 페이징 정보 계산 (폴링이 아닐 때만)
        boolean hasNext = false;
        Long nextCursor = null;

        if (since == null && messages.size() > size) {
            hasNext = true;
            messages = messages.subList(0, size);
            nextCursor = messages.get(messages.size() - 1).getMessageId();
        }

        // 5. ChatMessage -> ChatMessageResponse 변환
        List<ChatMessageResponse> messageResponses = messages.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());

        // 6. 페이징 정보 생성
        PaginationResponse pagination = PaginationResponse.of(
                nextCursor,
                hasNext,
                messageResponses.size()
        );

        log.info("메시지 목록 조회 완료: 총 {}개", messageResponses.size());

        return ChatMessageListResponse.of(messageResponses, pagination);
    }

    // ChatMessage -> ChatMessageResponse 변환
    private ChatMessageResponse convertToResponse(ChatMessage message) {
        // TODO: User 도메인 연동하여 실제 닉네임 조회
        String senderNickname = message.getSenderId() != null ? "사용자" : "시스템";

        // TODO: File 도메인 연동하여 파일 정보 조회
        ChatMessageResponse.FileInfo fileInfo = null;
        if (message.getFileId() != null) {
            fileInfo = ChatMessageResponse.FileInfo.builder()
                    .fileId(message.getFileId())
                    .fileName("파일명") // 임시값
                    .fileUrl("파일URL") // 임시값
                    .build();
        }

        return ChatMessageResponse.builder()
                .messageId(message.getMessageId())
                .senderId(message.getSenderId())
                .senderNickname(senderNickname)
                .messageType(message.getMessageType())
                .content(message.getContent())
                .file(fileInfo)
                .createdAt(message.getSentAt())
                .build();
    }
}