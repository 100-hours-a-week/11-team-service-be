package com.thunder11.scuad.chat.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
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

        // TODO: 1. jobMasterId 존재 여부 확인 (JobPosting 연동 후 구현)

        // TODO: 2. 내 공고 점수 조회 (JobPosting 연동 후 구현)
        Integer myScore = 0; // 임시값

        // 3. 채팅방 목록 조회 (size + 1개 조회하여 다음 페이지 존재 여부 확인)
        List<ChatRoom> chatRooms = chatRoomRepository.findByJobMasterIdWithCursor(
                jobMasterId,
                cursor,
                PageRequest.of(0, size + 1)
        );

        // 4. 다음 페이지 존재 여부 및 nextCursor 계산
        boolean hasNext = chatRooms.size() > size;
        if (hasNext) {
            chatRooms = chatRooms.subList(0, size); // 실제 size만큼만 반환
        }

        Long nextCursor = null;
        if (hasNext && !chatRooms.isEmpty()) {
            nextCursor = chatRooms.get(chatRooms.size() - 1).getChatRoomId();
        }

        // 5. ChatRoom -> ChatRoomSummaryResponse 변환
        List<ChatRoomSummaryResponse> summaries = chatRooms.stream()
                .map(room -> convertToSummary(room, userId))
                .collect(Collectors.toList());

        // 6. 페이징 정보 생성
        PaginationResponse pagination = PaginationResponse.of(
                nextCursor,
                hasNext,
                summaries.size()
        );

        log.info("채팅방 목록 조회 완료: 총 {}개", summaries.size());

        return ChatRoomListResponse.of(myScore, summaries, pagination);
    }

    // ChatRoom -> ChatRoomSummaryResponse 변환
    private ChatRoomSummaryResponse convertToSummary(ChatRoom room, Long userId) {
        // 현재 인원 수 조회
        long currentParticipants = chatRoomMemberRepository.countByChatRoomIdAndKickedAtIsNull(room.getChatRoomId());

        // 방장 정보 조회 (TODO: User 도메인 연동 후 닉네임 가져오기)
        String hostNickname = "방장"; // 임시값

        // 입장 가능 여부 판단
        boolean canJoin = determineCanJoin(room, currentParticipants, userId);

        // 입장 상태 판단
        String joinStatus = determineJoinStatus(room, currentParticipants, userId);

        return ChatRoomSummaryResponse.builder()
                .chatRoomId(room.getChatRoomId())
                .roomName(room.getRoomName())
                .roomGoal(room.getRoomGoal())
                .cutlineScore(room.getCutlineScore())
                .currentParticipants((int) currentParticipants)
                .maxParticipants(room.getMaxParticipants())
                .hostNickname(hostNickname)
                .preferredConditions(room.getPreferredConditions())
                .status(room.getStatus())
                .canJoin(canJoin)
                .joinStatus(joinStatus)
                .createdAt(room.getCreatedAt())
                .build();
    }

    // 입장 가능 여부 판단
    private boolean determineCanJoin(ChatRoom room, long currentParticipants, Long userId) {
        // 정원 초과
        if (currentParticipants >= room.getMaxParticipants()) {
            return false;
        }

        // 이미 참여 중인지 확인
        boolean alreadyJoined = chatRoomMemberRepository
                .findByChatRoomIdAndUserIdAndKickedAtIsNull(room.getChatRoomId(), userId)
                .isPresent();

        if (alreadyJoined) {
            return false;
        }

        // TODO: 커트라인 점수 확인 (JobPosting 연동 후 구현)
        // TODO: 같은 공고의 다른 방 참여 여부 확인 (JobPosting 연동 후 구현)

        return true;
    }

    // 입장 상태 판단
    private String determineJoinStatus(ChatRoom room, long currentParticipants, Long userId) {
        // 이미 참여 중
        boolean alreadyJoined = chatRoomMemberRepository
                .findByChatRoomIdAndUserIdAndKickedAtIsNull(room.getChatRoomId(), userId)
                .isPresent();

        if (alreadyJoined) {
            return "ALREADY_JOINED";
        }

        // 정원 초과
        if (currentParticipants >= room.getMaxParticipants()) {
            return "FULL";
        }

        // TODO: 커트라인 미달 체크
        // TODO: 같은 공고 다른 방 참여 중 체크

        return "AVAILABLE";
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

        return savedChatRoom.getChatRoomId();
    }
}