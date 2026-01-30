package com.thunder11.scuad.chat.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.thunder11.scuad.chat.domain.type.RoomStatus;
import com.thunder11.scuad.jobposting.domain.AiApplicantEvaluation;
import com.thunder11.scuad.jobposting.domain.JobApplication;
import com.thunder11.scuad.jobposting.domain.JobMaster;
import com.thunder11.scuad.jobposting.domain.type.ApplicationDocumentType;
import com.thunder11.scuad.jobposting.repository.AiApplicationEvaluationRepository;
import com.thunder11.scuad.jobposting.repository.ApplicationDocumentRepository;
import com.thunder11.scuad.jobposting.repository.JobApplicationRepository;
import com.thunder11.scuad.jobposting.repository.JobMasterRepository;
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
    private final JobMasterRepository jobMasterRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final AiApplicationEvaluationRepository aiApplicantEvaluationRepository;
    private final ApplicationDocumentRepository applicationDocumentRepository;

    // 사용자의 AI 평가 점수 조회 (private 헬퍼 메서드)
    private Integer getMyScore(Long userId, Long jobMasterId) {
        // 지원서 조회
        Optional<JobApplication> jobApplicationOpt = jobApplicationRepository
                .findByUserIdAndJobMasterId(userId, jobMasterId);

        if (jobApplicationOpt.isEmpty()) {
            log.debug("지원서 없음: userId={}, jobMasterId={}", userId, jobMasterId);
            return 0; // 지원하지 않은 경우 0점
        }

        // AI 평가 점수 조회
        Optional<AiApplicantEvaluation> evaluationOpt = aiApplicantEvaluationRepository
                .findByJobApplicationId(jobApplicationOpt.get().getId());

        if (evaluationOpt.isEmpty()) {
            log.debug("AI 평가 없음: jobApplicationId={}", jobApplicationOpt.get().getId());
            return 0; // AI 평가가 없는 경우 0점
        }

        Integer score = evaluationOpt.get().getOverallScore();
        log.debug("AI 점수 조회 완료: userId={}, jobMasterId={}, score={}",
                userId, jobMasterId, score);

        return score;
    }

    // 서류 제출 여부 확인 (private 헬퍼 메서드)
    private void validateDocumentsSubmitted(Long jobApplicationId, Long userId) {
        // 이력서 제출 확인
        boolean hasResume = applicationDocumentRepository
                .existsByJobApplicationIdAndDocType(jobApplicationId, ApplicationDocumentType.RESUME);

        if (!hasResume) {
            log.warn("이력서 미제출: userId={}, jobApplicationId={}", userId, jobApplicationId);
            throw new ApiException(ErrorCode.CHAT_ROOM_NO_RESUME);
        }

        // 포트폴리오 제출 확인
        boolean hasPortfolio = applicationDocumentRepository
                .existsByJobApplicationIdAndDocType(jobApplicationId, ApplicationDocumentType.PORTFOLIO);

        if (!hasPortfolio) {
            log.warn("포트폴리오 미제출: userId={}, jobApplicationId={}", userId, jobApplicationId);
            throw new ApiException(ErrorCode.CHAT_ROOM_NO_PORTFOLIO);
        }

        log.debug("서류 제출 확인 완료: userId={}, jobApplicationId={}", userId, jobApplicationId);
    }

    // 공고별 채팅방 목록 조회 (커서 기반 페이징)
    public ChatRoomListResponse getChatRoomsByJobPosting(
            Long jobMasterId,
            Long userId,
            Long cursor,
            int size
    ) {
        log.info("채팅방 목록 조회 시작: jobMasterId={}, userId={}, cursor={}, size={}",
                jobMasterId, userId, cursor, size);

        // 1. jobMasterId 존재 여부 확인
        if (!jobMasterRepository.existsByIdNotDeleted(jobMasterId)) {
            log.warn("존재하지 않는 공고: jobMasterId={}", jobMasterId);
            throw new ApiException(ErrorCode.JOB_POSTING_NOT_FOUND);
        }

        // 2. 내 공고 점수 조회
        Integer myScore = getMyScore(userId, jobMasterId);

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

        // 지원서 확인
        Optional<JobApplication> jobApplicationOpt = jobApplicationRepository
                .findByUserIdAndJobMasterId(userId, room.getJobMasterId());

        if (jobApplicationOpt.isEmpty()) {
            return false; // 지원하지 않은 경우
        }

        Long jobApplicationId = jobApplicationOpt.get().getId();

        // 서류 제출 확인 (이력서 + 포트폴리오 필수)
        boolean hasResume = applicationDocumentRepository
                .existsByJobApplicationIdAndDocType(jobApplicationId, ApplicationDocumentType.RESUME);
        boolean hasPortfolio = applicationDocumentRepository
                .existsByJobApplicationIdAndDocType(jobApplicationId, ApplicationDocumentType.PORTFOLIO);

        if (!hasResume || !hasPortfolio) {
            return false; // 서류 미제출
        }

        // AI 점수 확인
        Optional<AiApplicantEvaluation> evaluationOpt = aiApplicantEvaluationRepository
                .findByJobApplicationId(jobApplicationOpt.get().getId());

        if (evaluationOpt.isEmpty()) {
            return false; // AI 평가 없는 경우
        }

        // 커트라인 점수 확인
        Integer myScore = evaluationOpt.get().getOverallScore();
        if (myScore < room.getCutlineScore()) {
            return false; // 커트라인 미달
        }

        // 같은 공고의 다른 방 참여 여부 확인
        Optional<ChatRoomMember> otherRoomMember = chatRoomMemberRepository
                .findByJobApplicationIdAndNotKicked(jobApplicationOpt.get().getId());

        if (otherRoomMember.isPresent() && !otherRoomMember.get().getChatRoomId().equals(room.getChatRoomId())) {
            return false; // 같은 공고 다른 방 참여 중
        }

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

        // 지원서 확인
        Optional<JobApplication> jobApplicationOpt = jobApplicationRepository
                .findByUserIdAndJobMasterId(userId, room.getJobMasterId());

        if (jobApplicationOpt.isEmpty()) {
            return "NO_APPLICATION"; // 지원하지 않음
        }

        Long jobApplicationId = jobApplicationOpt.get().getId();

        // 서류 제출 확인 (이력서)
        boolean hasResume = applicationDocumentRepository
                .existsByJobApplicationIdAndDocType(jobApplicationId, ApplicationDocumentType.RESUME);

        if (!hasResume) {
            return "NO_RESUME"; // 이력서 미제출
        }

        // 서류 제출 확인 (포트폴리오)
        boolean hasPortfolio = applicationDocumentRepository
                .existsByJobApplicationIdAndDocType(jobApplicationId, ApplicationDocumentType.PORTFOLIO);

        if (!hasPortfolio) {
            return "NO_PORTFOLIO"; // 포트폴리오 미제출
        }

        // AI 점수 확인
        Optional<AiApplicantEvaluation> evaluationOpt = aiApplicantEvaluationRepository
                .findByJobApplicationId(jobApplicationOpt.get().getId());

        if (evaluationOpt.isEmpty()) {
            return "NO_SCORE"; // AI 평가 없음
        }

        // 커트라인 미달 체크
        Integer myScore = evaluationOpt.get().getOverallScore();
        if (myScore < room.getCutlineScore()) {
            return "CUTLINE_NOT_MET"; // 커트라인 미달
        }

        // 같은 공고 다른 방 참여 중 체크
        Optional<ChatRoomMember> otherRoomMember = chatRoomMemberRepository
                .findByJobApplicationIdAndNotKicked(jobApplicationOpt.get().getId());

        if (otherRoomMember.isPresent() && !otherRoomMember.get().getChatRoomId().equals(room.getChatRoomId())) {
            return "ALREADY_JOINED_OTHER"; // 같은 공고 다른 방 참여 중
        }

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

        // 1. 공고 존재 확인
        if (!jobMasterRepository.existsByIdNotDeleted(jobMasterId)) {
            log.warn("존재하지 않는 공고: jobMasterId={}", jobMasterId);
            throw new ApiException(ErrorCode.JOB_POSTING_NOT_FOUND);
        }

        // 2. 생성자 지원서 조회
        JobApplication jobApplication = jobApplicationRepository
                .findByUserIdAndJobMasterId(userId, jobMasterId)
                .orElseThrow(() -> {
                    log.warn("지원서 없음: userId={}, jobMasterId={}", userId, jobMasterId);
                    return new ApiException(ErrorCode.CHAT_ROOM_NO_APPLICATION);
                });

        // 3. 생성자 서류 제출 확인 (이력서 + 포트폴리오 필수)
        validateDocumentsSubmitted(jobApplication.getId(), userId);

        // 4. 생성자 AI 점수 확인
        AiApplicantEvaluation evaluation = aiApplicantEvaluationRepository
                .findByJobApplicationId(jobApplication.getId())
                .orElseThrow(() -> {
                    log.warn("AI 평가 없음: jobApplicationId={}", jobApplication.getId());
                    return new ApiException(ErrorCode.CHAT_ROOM_NO_SCORE);
                });

        Integer myScore = evaluation.getOverallScore();
        log.info("방장 AI 점수: userId={}, score={}", userId, myScore);

        // 5. 커트라인 검증 (본인 점수 이하여야 함)
        if (request.getCutlineScore() > myScore) {
            log.warn("커트라인이 본인 점수보다 높음: cutline={}, myScore={}",
                    request.getCutlineScore(), myScore);
            throw new ApiException(ErrorCode.CHAT_ROOM_CUTLINE_EXCEEDED);
        }

        // 6. 중복 방 생성 확인
        if (chatRoomRepository.existsByJobMasterIdAndCreatedByAndDeletedAtIsNull(jobMasterId, userId)) {
            log.warn("이미 해당 공고에 생성한 채팅방이 있습니다: jobMasterId={}, userId={}", jobMasterId, userId);
            throw new ApiException(ErrorCode.CHAT_ROOM_ALREADY_EXISTS);
        }

        // 7. 채팅방 생성
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
        log.info("채팅방 생성 완료: chatRoomId={}, cutline={}",
                savedChatRoom.getChatRoomId(), savedChatRoom.getCutlineScore());

        // 8. 방장을 멤버로 등록
        ChatRoomMember hostMember = ChatRoomMember.builder()
                .chatRoomId(savedChatRoom.getChatRoomId())
                .userId(userId)
                .jobApplicationId(jobApplication.getId())
                .role(MemberRole.HOST)
                .build();

        chatRoomMemberRepository.save(hostMember);
        log.info("방장 멤버 등록 완료: userId={}, chatRoomMemberId={}", userId, hostMember.getChatRoomMemberId());

        // TODO: 9. 시스템 메시지 생성 ("채팅방이 생성되었습니다")

        return savedChatRoom.getChatRoomId();
    }

    // 채팅방 상세 정보 조회
    public ChatRoomDetailResponse getChatRoomDetail(Long chatRoomId, Long userId) {
        log.info("채팅방 상세 조회 시작: chatRoomId={}, userId={}", chatRoomId, userId);

        // 1. 채팅방 존재 확인
        ChatRoom chatRoom = chatRoomRepository.findByIdNotDeleted(chatRoomId)
                .orElseThrow(() -> new ApiException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        // TODO: 2. 권한 확인 (참여자만 조회 가능)
        // boolean isMember = chatRoomMemberRepository
        //     .findByChatRoomIdAndUserIdAndKickedAtIsNull(chatRoomId, userId)
        //     .isPresent();
        // if (!isMember) {
        //     throw new ApiException(ErrorCode.CHAT_ROOM_ACCESS_DENIED);
        // }

        // 3. 현재 인원 수 조회
        long memberCount = chatRoomMemberRepository.countByChatRoomIdAndKickedAtIsNull(chatRoomId);

        // 4. 공고 정보 조회
        JobMaster jobMaster = jobMasterRepository.findByIdWithDetails(chatRoom.getJobMasterId())
                .orElseThrow(() -> new ApiException(ErrorCode.JOB_POSTING_NOT_FOUND));

        ChatRoomDetailResponse.JobMasterSummary jobMasterSummary = ChatRoomDetailResponse.JobMasterSummary.builder()
                .jobMasterId(jobMaster.getId())
                .companyName(jobMaster.getCompany().getName())
                .jobTitle(jobMaster.getJobTitle())
                .build();

        // 5. 응답 생성
        ChatRoomDetailResponse response = ChatRoomDetailResponse.builder()
                .chatRoomId(chatRoom.getChatRoomId())
                .roomName(chatRoom.getRoomName())
                .roomGoal(chatRoom.getRoomGoal())
                .cutlineScore(chatRoom.getCutlineScore())
                .currentParticipants((int) memberCount)
                .maxParticipants(chatRoom.getMaxParticipants())
                .preferredConditions(chatRoom.getPreferredConditions())
                .status(chatRoom.getStatus())
                .jobMaster(jobMasterSummary)
                .memberCount((int) memberCount)
                .createdAt(chatRoom.getCreatedAt())
                .build();

        log.info("채팅방 상세 조회 완료: chatRoomId={}", chatRoomId);

        return response;
    }

    // 채팅방 입장
    @Transactional
    public void joinChatRoom(Long chatRoomId, Long userId) {
        log.info("채팅방 입장 시작: chatRoomId={}, userId={}", chatRoomId, userId);

        // 1. 채팅방 존재 확인
        ChatRoom chatRoom = chatRoomRepository.findByIdNotDeleted(chatRoomId)
                .orElseThrow(() -> new ApiException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        // 2. 채팅방 상태 확인 (ACTIVE만 입장 가능)
        if (chatRoom.getStatus() != RoomStatus.ACTIVE) {
            throw new ApiException(ErrorCode.CHAT_ROOM_NOT_FOUND);
        }

        // 3. 이미 참여 중인지 확인
        if (chatRoomMemberRepository.findByChatRoomIdAndUserIdAndKickedAtIsNull(chatRoomId, userId).isPresent()) {
            log.warn("이미 참여 중인 채팅방: chatRoomId={}, userId={}", chatRoomId, userId);
            throw new ApiException(ErrorCode.CHAT_ROOM_ALREADY_JOINED);
        }

        // 4. 정원 확인
        long currentParticipants = chatRoomMemberRepository.countByChatRoomIdAndKickedAtIsNull(chatRoomId);
        if (currentParticipants >= chatRoom.getMaxParticipants()) {
            log.warn("정원 초과: chatRoomId={}, current={}, max={}",
                    chatRoomId, currentParticipants, chatRoom.getMaxParticipants());
            throw new ApiException(ErrorCode.CHAT_ROOM_FULL);
        }

        // 5. 입장자 지원서 조회
        JobApplication jobApplication = jobApplicationRepository
                .findByUserIdAndJobMasterId(userId, chatRoom.getJobMasterId())
                .orElseThrow(() -> {
                    log.warn("지원서 없음: userId={}, jobMasterId={}", userId, chatRoom.getJobMasterId());
                    return new ApiException(ErrorCode.CHAT_ROOM_NO_APPLICATION);
                });

        // 6. 강퇴 여부 확인
        if (chatRoomMemberRepository.existsKickedMember(chatRoomId, userId)) {
            log.warn("강퇴된 사용자의 재입장 시도: chatRoomId={}, userId={}", chatRoomId, userId);
            throw new ApiException(ErrorCode.CHAT_MEMBER_KICKED);
        }

        // 7. 같은 공고 다른 방 참여 확인
        Optional<ChatRoomMember> otherRoomMember = chatRoomMemberRepository
                .findByJobApplicationIdAndNotKicked(jobApplication.getId());

        if (otherRoomMember.isPresent() && !otherRoomMember.get().getChatRoomId().equals(chatRoomId)) {
            log.warn("같은 공고의 다른 방 참여 중: userId={}, otherChatRoomId={}",
                    userId, otherRoomMember.get().getChatRoomId());
            throw new ApiException(ErrorCode.CHAT_ROOM_ALREADY_JOINED_OTHER);
        }

        // 8. 서류 제출 확인 (이력서 + 포트폴리오 필수)
        validateDocumentsSubmitted(jobApplication.getId(), userId);

        // 9. 커트라인 점수 확인
        AiApplicantEvaluation evaluation = aiApplicantEvaluationRepository
                .findByJobApplicationId(jobApplication.getId())
                .orElseThrow(() -> {
                    log.warn("AI 평가 없음: jobApplicationId={}", jobApplication.getId());
                    return new ApiException(ErrorCode.CHAT_ROOM_NO_SCORE);
                });

        Integer myScore = evaluation.getOverallScore();
        if (myScore < chatRoom.getCutlineScore()) {
            log.warn("커트라인 미달: userId={}, myScore={}, cutline={}",
                    userId, myScore, chatRoom.getCutlineScore());
            throw new ApiException(ErrorCode.CHAT_ROOM_CUTLINE_NOT_MET);
        }

        log.info("커트라인 통과: userId={}, myScore={}, cutline={}",
                userId, myScore, chatRoom.getCutlineScore());

        // 10. 멤버 등록
        ChatRoomMember member = ChatRoomMember.builder()
                .chatRoomId(chatRoomId)
                .userId(userId)
                .jobApplicationId(jobApplication.getId())
                .role(MemberRole.MEMBER)
                .build();

        chatRoomMemberRepository.save(member);
        log.info("채팅방 입장 완료: chatRoomId={}, userId={}, chatRoomMemberId={}",
                chatRoomId, userId, member.getChatRoomMemberId());

        // TODO: 11. 시스템 메시지 생성 ("OO님이 입장했습니다")
    }

    // 채팅방 퇴장
    @Transactional
    public void leaveChatRoom(Long chatRoomId, Long userId) {
        log.info("채팅방 퇴장 시작: chatRoomId={}, userId={}", chatRoomId, userId);

        // 1. 채팅방 존재 확인
        ChatRoom chatRoom = chatRoomRepository.findByIdNotDeleted(chatRoomId)
                .orElseThrow(() -> new ApiException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        // 2. 멤버십 조회
        ChatRoomMember member = chatRoomMemberRepository
                .findByChatRoomIdAndUserIdAndKickedAtIsNull(chatRoomId, userId)
                .orElseThrow(() -> new ApiException(ErrorCode.CHAT_MEMBER_NOT_FOUND));

        // 3. 방장은 퇴장 불가 (방을 종료해야 함)
        if (member.getRole() == MemberRole.HOST) {
            log.warn("방장의 퇴장 시도: chatRoomId={}, userId={}", chatRoomId, userId);
            throw new ApiException(ErrorCode.CHAT_ROOM_HOST_ONLY);
        }

        // 4. 멤버 삭제
        chatRoomMemberRepository.delete(member);
        log.info("채팅방 퇴장 완료: chatRoomId={}, userId={}, chatRoomMemberId={}",
                chatRoomId, userId, member.getChatRoomMemberId());

        // TODO: 5. 시스템 메시지 생성 ("OO님이 퇴장했습니다")
    }

    // 멤버 강퇴
    @Transactional
    public void kickMember(Long chatRoomId, Long hostUserId, Long chatRoomMemberId) {
        log.info("멤버 강퇴 시작: chatRoomId={}, hostUserId={}, chatRoomMemberId={}",
                chatRoomId, hostUserId, chatRoomMemberId);

        // 1. 채팅방 존재 확인
        chatRoomRepository.findByIdNotDeleted(chatRoomId)
                .orElseThrow(() -> new ApiException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        // 2. 방장 권한 확인
        boolean isHost = chatRoomMemberRepository.isHostOfRoom(chatRoomId, hostUserId);
        if (!isHost) {
            log.warn("방장이 아닌 사용자의 강퇴 시도: chatRoomId={}, userId={}", chatRoomId, hostUserId);
            throw new ApiException(ErrorCode.CHAT_ROOM_HOST_ONLY);
        }

        // 3. 강퇴 대상 멤버 조회
        ChatRoomMember targetMember = chatRoomMemberRepository
                .findByChatRoomMemberIdAndKickedAtIsNull(chatRoomMemberId)
                .orElseThrow(() -> new ApiException(ErrorCode.CHAT_MEMBER_NOT_FOUND));

        // 4. 같은 채팅방인지 확인
        if (!targetMember.getChatRoomId().equals(chatRoomId)) {
            log.warn("다른 채팅방의 멤버 강퇴 시도: chatRoomId={}, targetChatRoomId={}",
                    chatRoomId, targetMember.getChatRoomId());
            throw new ApiException(ErrorCode.CHAT_MEMBER_NOT_FOUND);
        }

        // 5. 방장 자신 강퇴 불가
        if (targetMember.getUserId().equals(hostUserId)) {
            log.warn("방장 자신을 강퇴 시도: chatRoomId={}, userId={}", chatRoomId, hostUserId);
            throw new ApiException(ErrorCode.INVALID_INPUT_VALUE);
        }

        // 6. 방장 강퇴 불가
        if (targetMember.getRole() == MemberRole.HOST) {
            log.warn("방장을 강퇴 시도: chatRoomId={}, targetUserId={}", chatRoomId, targetMember.getUserId());
            throw new ApiException(ErrorCode.CHAT_ROOM_HOST_ONLY);
        }

        // 7. 강퇴 처리 (kicked_at 설정)
        targetMember.kick();
        chatRoomMemberRepository.save(targetMember);
        log.info("멤버 강퇴 완료: chatRoomId={}, kickedUserId={}", chatRoomId, targetMember.getUserId());

        // TODO: 8. 시스템 메시지 생성 ("OO님이 강퇴되었습니다")
    }

    // 채팅방 종료
    @Transactional
    public void closeChatRoom(Long chatRoomId, Long userId) {
        log.info("채팅방 종료 시작: chatRoomId={}, userId={}", chatRoomId, userId);

        // 1. 채팅방 존재 확인
        ChatRoom chatRoom = chatRoomRepository.findByIdNotDeleted(chatRoomId)
                .orElseThrow(() -> new ApiException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        // 2. 방장 권한 확인
        boolean isHost = chatRoomMemberRepository.isHostOfRoom(chatRoomId, userId);
        if (!isHost) {
            log.warn("방장이 아닌 사용자의 방 종료 시도: chatRoomId={}, userId={}", chatRoomId, userId);
            throw new ApiException(ErrorCode.CHAT_ROOM_HOST_ONLY);
        }

        // 3. 이미 종료된 방인지 확인
        if (chatRoom.getStatus() == RoomStatus.CLOSED) {
            log.warn("이미 종료된 채팅방: chatRoomId={}", chatRoomId);
            throw new ApiException(ErrorCode.CHAT_ROOM_NOT_FOUND);
        }

        // 4. 채팅방 종료
        chatRoom.close();
        chatRoomRepository.save(chatRoom);
        log.info("채팅방 종료 완료: chatRoomId={}", chatRoomId);

        // TODO: 5. 시스템 메시지 생성 ("채팅방이 종료되었습니다")
    }
}