package com.thunder11.scuad.chat.controller;

import com.thunder11.scuad.chat.dto.request.ChatRoomCreateRequest;
import com.thunder11.scuad.chat.dto.request.MessageSendRequest;
import com.thunder11.scuad.chat.dto.response.*;
import com.thunder11.scuad.chat.domain.type.MessageType;
import com.thunder11.scuad.chat.service.ChatMemberDocumentService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.thunder11.scuad.auth.security.UserPrincipal;
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
    private final com.thunder11.scuad.file.service.FileStorageService fileStorageService;
    private final ChatMemberDocumentService chatMemberDocumentService;

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

    // 채팅방 멤버 목록 조회
    @GetMapping("/chat-rooms/{chatRoomId}/members")
    public ApiResponse<ChatRoomMemberListResponse> getChatRoomMembers(
            @PathVariable Long chatRoomId,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        log.info("GET /api/v1/chat-rooms/{}/members - userId={}",
                chatRoomId, userPrincipal.getUserId());

        ChatRoomMemberListResponse response = chatRoomService.getChatRoomMembers(
                chatRoomId,
                userPrincipal.getUserId()
        );

        return ApiResponse.of(
                HttpStatus.OK.value(),
                "SUCCESS",
                "멤버 목록 조회 성공",
                response
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

    // 메시지 전송 (multipart/form-data 지원)
    // 수정 이유: FILE 타입 메시지 전송 시 파일을 S3에 업로드하고 fileId를 생성하여
    //           MessageSendRequest에 포함시켜야 서비스에서 정상 처리 가능
    @PostMapping(value = "/chat-rooms/{chatRoomId}/messages", consumes = {"multipart/form-data"})
    public ApiResponse<ChatMessageResponse> sendMessage(
            @PathVariable Long chatRoomId,
            @RequestParam("messageType") String messageType,
            @RequestParam(value = "content", required = false) String content,
            @RequestParam(value = "file", required = false) org.springframework.web.multipart.MultipartFile file,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        log.info("POST /api/v1/chat-rooms/{}/messages - messageType={}, content={}, hasFile={}, userId={}",
                chatRoomId, messageType, content, (file != null), userPrincipal.getUserId());

        // MessageType 변환
        MessageType msgType = MessageType.valueOf(messageType.toUpperCase());
        
        Long fileId = null;
        
        // FILE 타입인 경우 파일 업로드 처리
        if (msgType == MessageType.FILE) {
            if (file == null || file.isEmpty()) {
                log.warn("FILE 타입 메시지인데 파일이 없음: chatRoomId={}, userId={}", chatRoomId, userPrincipal.getUserId());
                throw new com.thunder11.scuad.common.exception.ApiException(
                    com.thunder11.scuad.common.exception.ErrorCode.INVALID_INPUT_VALUE
                );
            }
            
            // 파일 S3 업로드 및 fileId 생성
            try {
                com.thunder11.scuad.file.domain.FileObject fileObject = fileStorageService.uploadFile(
                    file, 
                    "chat-files/" + chatRoomId
                );
                fileId = fileObject.getId();
                log.info("파일 업로드 완료: fileId={}, originalName={}, size={}", 
                    fileId, file.getOriginalFilename(), file.getSize());
            } catch (Exception e) {
                log.error("파일 업로드 실패: chatRoomId={}, userId={}, error={}", 
                    chatRoomId, userPrincipal.getUserId(), e.getMessage());
                throw e;
            }
        }

        // DTO 생성
        MessageSendRequest request = MessageSendRequest.builder()
                .messageType(msgType)
                .content(content)
                .fileId(fileId)
                .build();

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

    // 채팅방 종료
    @PatchMapping("/chat-rooms/{chatRoomId}/close")
    public ApiResponse<Void> closeChatRoom(
            @PathVariable Long chatRoomId,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        log.info("PATCH /api/v1/chat-rooms/{}/close - userId={}",
                chatRoomId, userPrincipal.getUserId());

        chatRoomService.closeChatRoom(chatRoomId, userPrincipal.getUserId());

        return ApiResponse.of(
                HttpStatus.OK.value(),
                "CHAT_ROOM_CLOSED",
                "채팅방 종료 완료"
        );
    }

    // 내가 참여 중인 채팅방 목록 조회
    @GetMapping("/users/me/chat-rooms")
    public ApiResponse<MyChatRoomListResponse> getMyChatRooms(
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        log.info("GET /api/v1/users/me/chat-rooms - cursor={}, size={}, userId={}",
                cursor, size, userPrincipal.getUserId());

        MyChatRoomListResponse response = chatRoomService.getMyChatRooms(
                userPrincipal.getUserId(),
                cursor,
                size
        );

        return ApiResponse.of(
                HttpStatus.OK.value(),
                "SUCCESS",
                "내 채팅방 목록 조회 성공",
                response
        );
    }

    // 채팅방 멤버 이력서 조회
    @GetMapping("/chat-rooms/{chatRoomId}/members/{chatRoomMemberId}/documents/resume")
    public ApiResponse<MemberDocumentResponse> getMemberResume(
            @PathVariable Long chatRoomId,
            @PathVariable Long chatRoomMemberId,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        log.info("GET /api/v1/chat-rooms/{}/members/{}/documents/resume - userId={}",
                chatRoomId, chatRoomMemberId, userPrincipal.getUserId());

        MemberDocumentResponse response = chatMemberDocumentService.getMemberResume(
                chatRoomId,
                userPrincipal.getUserId(),
                chatRoomMemberId
        );

        return ApiResponse.of(
                HttpStatus.OK.value(),
                "SUCCESS",
                "이력서 조회 성공",
                response
        );
    }

    // 채팅방 멤버 포트폴리오 조회
    @GetMapping("/chat-rooms/{chatRoomId}/members/{chatRoomMemberId}/documents/portfolio")
    public ApiResponse<MemberDocumentResponse> getMemberPortfolio(
            @PathVariable Long chatRoomId,
            @PathVariable Long chatRoomMemberId,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        log.info("GET /api/v1/chat-rooms/{}/members/{}/documents/portfolio - userId={}",
                chatRoomId, chatRoomMemberId, userPrincipal.getUserId());

        MemberDocumentResponse response = chatMemberDocumentService.getMemberPortfolio(
                chatRoomId,
                userPrincipal.getUserId(),
                chatRoomMemberId
        );

        return ApiResponse.of(
                HttpStatus.OK.value(),
                "SUCCESS",
                "포트폴리오 조회 성공",
                response
        );
    }
}