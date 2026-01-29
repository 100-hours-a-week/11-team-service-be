package com.thunder11.scuad.chat.controller;

import com.thunder11.scuad.chat.dto.request.ChatRoomCreateRequest;
import com.thunder11.scuad.chat.dto.request.MessageSendRequest;
import com.thunder11.scuad.chat.dto.response.ChatMessageListResponse;
import com.thunder11.scuad.chat.dto.response.ChatMessageResponse;
import com.thunder11.scuad.chat.dto.response.ChatRoomDetailResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.thunder11.scuad.auth.security.UserPrincipal;
import com.thunder11.scuad.chat.dto.response.ChatRoomListResponse;
import com.thunder11.scuad.chat.service.ChatMessageService;
import com.thunder11.scuad.chat.service.ChatRoomService;
import com.thunder11.scuad.common.response.ApiResponse;

// 채팅방 관련 API 컨트롤러
@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ChatRoomController {

    private final ChatRoomService chatRoomService;
    private final ChatMessageService chatMessageService;

    // 공고별 채팅방 목록 조회
    @GetMapping("/job-postings/{jobMasterId}/chat-rooms")
    public ApiResponse<ChatRoomListResponse> getChatRoomList(
            @PathVariable Long jobMasterId,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        log.info("GET /api/v1/job-postings/{}/chat-rooms - cursor={}, size={}, userId={}",
                jobMasterId, cursor, size, userPrincipal.getUserId());

        ChatRoomListResponse response = chatRoomService.getChatRoomsByJobPosting(
                jobMasterId,
                userPrincipal.getUserId(),
                cursor,
                size
        );

        return ApiResponse.of(
                HttpStatus.OK.value(),
                "SUCCESS",
                "채팅방 목록 조회 성공",
                response
        );
    }

    // 채팅방 생성
    @PostMapping("/job-postings/{jobMasterId}/chat-rooms")
    public ApiResponse<Long> createChatRoom(
            @PathVariable Long jobMasterId,
            @Valid @RequestBody ChatRoomCreateRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        log.info("POST /api/v1/job-postings/{}/chat-rooms - roomName={}, userId={}",
                jobMasterId, request.getRoomName(), userPrincipal.getUserId());

        Long chatRoomId = chatRoomService.createChatRoom(
                jobMasterId,
                userPrincipal.getUserId(),
                request
        );

        return ApiResponse.of(
                HttpStatus.CREATED.value(),
                "CHAT_ROOM_CREATED",
                "채팅방 생성 완료",
                chatRoomId
        );
    }

    // 채팅방 상세 정보 조회
    @GetMapping("/chat-rooms/{chatRoomId}")
    public ApiResponse<ChatRoomDetailResponse> getChatRoomDetail(
            @PathVariable Long chatRoomId,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        log.info("GET /api/v1/chat-rooms/{} - userId={}",
                chatRoomId, userPrincipal.getUserId());

        ChatRoomDetailResponse response = chatRoomService.getChatRoomDetail(
                chatRoomId,
                userPrincipal.getUserId()
        );

        return ApiResponse.of(
                HttpStatus.OK.value(),
                "SUCCESS",
                "채팅방 상세 조회 성공",
                response
        );
    }

    // 채팅방 입장
    @PostMapping("/chat-rooms/{chatRoomId}/members")
    public ApiResponse<Void> joinChatRoom(
            @PathVariable Long chatRoomId,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        log.info("POST /api/v1/chat-rooms/{}/members - userId={}",
                chatRoomId, userPrincipal.getUserId());

        chatRoomService.joinChatRoom(chatRoomId, userPrincipal.getUserId());

        return ApiResponse.of(
                HttpStatus.OK.value(),
                "CHAT_ROOM_JOINED",
                "채팅방 입장 완료"
        );
    }

    // 채팅 메시지 목록 조회
    @GetMapping("/chat-rooms/{chatRoomId}/messages")
    public ApiResponse<ChatMessageListResponse> getMessages(
            @PathVariable Long chatRoomId,
            @RequestParam(required = false) Long cursor,
            @RequestParam(required = false) Long since,
            @RequestParam(defaultValue = "50") int size,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        log.info("GET /api/v1/chat-rooms/{}/messages - cursor={}, since={}, size={}, userId={}",
                chatRoomId, cursor, since, size, userPrincipal.getUserId());

        ChatMessageListResponse response = chatMessageService.getMessages(
                chatRoomId,
                userPrincipal.getUserId(),
                cursor,
                since,
                size
        );

        return ApiResponse.of(
                HttpStatus.OK.value(),
                "SUCCESS",
                "메시지 목록 조회 성공",
                response
        );
    }

    // 메시지 전송
    @PostMapping("/chat-rooms/{chatRoomId}/messages")
    public ApiResponse<ChatMessageResponse> sendMessage(
            @PathVariable Long chatRoomId,
            @Valid @RequestBody MessageSendRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        log.info("POST /api/v1/chat-rooms/{}/messages - messageType={}, userId={}",
                chatRoomId, request.getMessageType(), userPrincipal.getUserId());

        ChatMessageResponse response = chatMessageService.sendMessage(
                chatRoomId,
                userPrincipal.getUserId(),
                request
        );

        return ApiResponse.of(
                HttpStatus.CREATED.value(),
                "MESSAGE_SENT",
                "메시지 전송 완료",
                response
        );
    }

    // 채팅방 퇴장
    @DeleteMapping("/chat-rooms/{chatRoomId}/members/me")
    public ApiResponse<Void> leaveChatRoom(
            @PathVariable Long chatRoomId,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        log.info("DELETE /api/v1/chat-rooms/{}/members/me - userId={}",
                chatRoomId, userPrincipal.getUserId());

        chatRoomService.leaveChatRoom(chatRoomId, userPrincipal.getUserId());

        return ApiResponse.of(
                HttpStatus.OK.value(),
                "CHAT_ROOM_LEFT",
                "채팅방 퇴장 완료"
        );
    }

    // 멤버 강퇴
    @DeleteMapping("/chat-rooms/{chatRoomId}/members/{chatRoomMemberId}")
    public ApiResponse<Void> kickMember(
            @PathVariable Long chatRoomId,
            @PathVariable Long chatRoomMemberId,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        log.info("DELETE /api/v1/chat-rooms/{}/members/{} - hostUserId={}",
                chatRoomId, chatRoomMemberId, userPrincipal.getUserId());

        chatRoomService.kickMember(chatRoomId, userPrincipal.getUserId(), chatRoomMemberId);

        return ApiResponse.of(
                HttpStatus.OK.value(),
                "MEMBER_KICKED",
                "멤버 강퇴 완료"
        );
    }
}