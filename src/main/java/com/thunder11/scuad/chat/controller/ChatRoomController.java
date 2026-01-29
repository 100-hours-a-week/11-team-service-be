package com.thunder11.scuad.chat.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.thunder11.scuad.auth.security.UserPrincipal;
import com.thunder11.scuad.chat.dto.request.ChatRoomCreateRequest;
import com.thunder11.scuad.chat.dto.response.ChatRoomListResponse;
import com.thunder11.scuad.chat.service.ChatRoomService;
import com.thunder11.scuad.common.response.ApiResponse;

// 채팅방 관련 API 컨트롤러
@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ChatRoomController {

    private final ChatRoomService chatRoomService;

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
}