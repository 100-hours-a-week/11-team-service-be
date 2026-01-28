package com.thunder11.scuad.chat.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.thunder11.scuad.chat.domain.ChatRoom;
import com.thunder11.scuad.chat.domain.ChatRoomMember;
import com.thunder11.scuad.chat.domain.type.MemberRole;
import com.thunder11.scuad.chat.dto.request.ChatRoomCreateRequest;
import com.thunder11.scuad.chat.dto.response.*;
import com.thunder11.scuad.chat.repository.ChatRoomMemberRepository;
import com.thunder11.scuad.chat.repository.ChatRoomRepository;
import com.thunder11.scuad.common.exception.ApiException;
import com.thunder11.scuad.common.exception.ErrorCode;

// 채팅방 관련 비즈니스 로직 처리
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;

    // 공고별 채팅방 목록 조회 (커서 기반 페이징)
    public ChatRoomListResponse getChatRoomsByJobPosting(
            Long jobMasterId,
            Long userId,
            Long cursor,
            int size
    ) {
        log.info("채팅방 목록 조회 시작: jobMasterId={}, userId={}, cursor={}, size={}",
                jobMasterId, userId, cursor, size);

        // TODO: 1. jobMasterId 존재 여부 확인 (JobMaster 연동 필요)
        // 현재는 생략 - JobPosting 도메인 연동 후 구현

        // TODO: 2. 내 공고 점수 조회 (JobApplication 연동 필요)
        // 현재는 임시로 0점 설정
        Integer myScore = 0;

        // TODO: 3. 채팅방 목록 실제 DB 조회 및 페이징 구현
        // 현재는 빈 리스트 반환 - 다음 커밋에서 구현
        List<ChatRoomSummaryResponse> chatRooms = new ArrayList<>();

        // 4. 페이징 정보 생성
        PaginationResponse pagination = PaginationResponse.of(
                null,  // nextCursor
                false, // hasNext
                chatRooms.size()
        );

        log.info("채팅방 목록 조회 완료: 총 {}개", chatRooms.size());

        return ChatRoomListResponse.of(myScore, chatRooms, pagination);
    }

    // 채팅방 생성
    @Transactional
    public Long createChatRoom(
            Long jobMasterId,
            Long userId,
            ChatRoomCreateRequest request
    ) {
        log.info("채팅방 생성 시작: jobMasterId={}, userId={}, roomName={}",
                jobMasterId, userId, request.getRoomName());

        // TODO: 1. 공고 존재 확인 (JobPosting 도메인 연동 필요)

        // TODO: 2. 생성자 서류 제출 확인 (JobApplication 연동 필요)

        // TODO: 3. 생성자 AI 점수 확인 (JobApplication 연동 필요)

        // TODO: 4. 커트라인 검증 (본인 점수 이하여야 함)

        // 5. 중복 방 생성 확인
        if (chatRoomRepository.existsByJobMasterIdAndCreatedByAndDeletedAtIsNull(jobMasterId, userId)) {
            log.warn("이미 해당 공고에 생성한 채팅방이 있습니다: jobMasterId={}, userId={}", jobMasterId, userId);
            throw new ApiException(ErrorCode.CHAT_ROOM_ALREADY_EXISTS);
        }

        // 6. 채팅방 생성
        ChatRoom chatRoom = ChatRoom.builder()
                .jobMasterId(jobMasterId)
                .createdBy(userId)
                .roomName(request.getRoomName())
                .maxParticipants(request.getMaxParticipants())
                .roomGoal(request.getRoomGoal())
                .cutlineScore(request.getCutlineScore())
                .preferredConditions(request.getPreferredConditions())
                .build();

        ChatRoom savedChatRoom = chatRoomRepository.save(chatRoom);
        log.info("채팅방 생성 완료: chatRoomId={}", savedChatRoom.getChatRoomId());

        // 7. 방장을 멤버로 등록
        // TODO: jobApplicationId 조회 필요 (JobApplication 연동 필요)
        Long jobApplicationId = 1L; // 임시값

        ChatRoomMember hostMember = ChatRoomMember.builder()
                .chatRoomId(savedChatRoom.getChatRoomId())
                .userId(userId)
                .jobApplicationId(jobApplicationId)
                .role(MemberRole.HOST)
                .build();

        chatRoomMemberRepository.save(hostMember);
        log.info("방장 멤버 등록 완료: userId={}, chatRoomMemberId={}", userId, hostMember.getChatRoomMemberId());

        // TODO: 8. 시스템 메시지 생성 ("채팅방이 생성되었습니다")
        // 메시지 전송 API 구현 후 추가

        return savedChatRoom.getChatRoomId();
    }
}