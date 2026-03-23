package com.thunder11.scuad.chat.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.thunder11.scuad.auth.repository.UserRepository;
import com.thunder11.scuad.chat.domain.ChatMessage;
import com.thunder11.scuad.chat.domain.type.RoomStatus;
import com.thunder11.scuad.chat.repository.ChatMessageRepository;
import com.thunder11.scuad.jobposting.domain.AiApplicantEvaluation;
import com.thunder11.scuad.jobposting.domain.JobApplication;
import com.thunder11.scuad.jobposting.domain.JobMaster;
import com.thunder11.scuad.jobposting.domain.type.ApplicationDocumentType;
import com.thunder11.scuad.jobposting.repository.AiApplicationEvaluationRepository;
import com.thunder11.scuad.jobposting.repository.ApplicationDocumentRepository;
import com.thunder11.scuad.jobposting.repository.JobApplicationRepository;
import com.thunder11.scuad.file.service.S3FileManagementService;
import com.thunder11.scuad.jobposting.repository.JobMasterRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.thunder11.scuad.notification.event.ChatRoomNotificationEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.thunder11.scuad.chat.domain.ChatRoom;
import com.thunder11.scuad.chat.domain.ChatRoomMember;
import com.thunder11.scuad.chat.domain.type.MemberRole;
import com.thunder11.scuad.chat.dto.ChatRoomPreloadedData;
import com.thunder11.scuad.chat.dto.JoinEligibility;
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
    private final AiApplicationEvaluationRepository aiApplicationEvaluationRepository;;
    private final ApplicationDocumentRepository applicationDocumentRepository;
    private final UserRepository userRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final S3FileManagementService s3FileManagementService;
    private final ApplicationEventPublisher eventPublisher;

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
        Optional<AiApplicantEvaluation> evaluationOpt = aiApplicationEvaluationRepository
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
    // 수정 근거: 이력서는 필수이지만 포트폴리오는 선택 제출이므로 이력서만 검증
    private void validateDocumentsSubmitted(Long jobApplicationId, Long userId) {
        // 이력서 제출 확인 (필수)
        boolean hasResume = applicationDocumentRepository
                .existsByJobApplicationIdAndDocType(jobApplicationId, ApplicationDocumentType.RESUME);

        if (!hasResume) {
            log.warn("이력서 미제출: userId={}, jobApplicationId={}", userId, jobApplicationId);
            throw new ApiException(ErrorCode.CHAT_ROOM_NO_RESUME);
        }

        // 포트폴리오는 선택 제출이므로 검증하지 않음
        log.debug("서류 제출 확인 완료 (이력서): userId={}, jobApplicationId={}", userId, jobApplicationId);
    }

    // ChatRoom -> ChatRoomSummaryResponse 변환
    // 개선: stream 밖에서 미리 조회한 ChatRoomPreloadedData를 받아 DB 재조회 없이 Map 조회만 수행
    private ChatRoomSummaryResponse convertToSummary(ChatRoom room, Long userId, ChatRoomPreloadedData data) {
        long currentParticipants = data.participantCountMap().getOrDefault(room.getChatRoomId(), 0L);
        String hostNickname = data.hostNicknameMap().getOrDefault(room.getCreatedBy(), "알 수 없음");
        JoinEligibility eligibility = determineJoinEligibility(room, currentParticipants, data);

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
                .canJoin(eligibility.isCanJoin())
                .joinStatus(eligibility.getStatus())
                .createdAt(room.getCreatedAt())
                .build();
    }

    // 입장 가능 여부 및 상태를 한 번에 판단
    // 개선: Map/Set 조회만 수행 — 기존에는 채팅방마다 최대 6번 DB 조회 발생
    private JoinEligibility determineJoinEligibility(ChatRoom room, long currentParticipants, ChatRoomPreloadedData data) {
        // 1. 이미 참여 중인지 확인
        if (data.joinedRoomIds().contains(room.getChatRoomId())) {
            return JoinEligibility.unavailable("ALREADY_JOINED");
        }

        // 2. 정원 초과 확인
        if (currentParticipants >= room.getMaxParticipants()) {
            return JoinEligibility.unavailable("FULL");
        }

        // 3. 강퇴 이력 확인
        if (data.kickedRoomIds().contains(room.getChatRoomId())) {
            return JoinEligibility.unavailable("KICKED");
        }

        // 4. 지원서 확인
        if (data.jobApplication() == null) {
            return JoinEligibility.unavailable("NO_APPLICATION");
        }

        // 5. 서류 제출 확인 (이력서만 필수, 포트폴리오는 선택)
        if (!data.hasResume()) {
            return JoinEligibility.unavailable("NO_RESUME");
        }

        // 6. AI 점수 확인
        if (data.evaluation() == null) {
            return JoinEligibility.unavailable("NO_SCORE");
        }

        // 7. 커트라인 점수 확인
        if (data.evaluation().getOverallScore() < room.getCutlineScore()) {
            return JoinEligibility.unavailable("CUTLINE_NOT_MET");
        }

        // 8. 같은 공고의 다른 방 참여 여부 확인
        if (data.otherRoomMember().isPresent()
                && !data.otherRoomMember().get().getChatRoomId().equals(room.getChatRoomId())) {
            return JoinEligibility.unavailable("ALREADY_JOINED_OTHER");
        }

        return JoinEligibility.available();
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
        // 개선: stream 시작 전 공통 데이터를 IN 쿼리로 일괄 조회 후 Map/Set으로 재사용
        //       기존: 채팅방마다 8번 DB 조회 → 총 4 + (8 × N)번
        //       개선: stream 밖 IN 쿼리 8번 고정 → 총 4 + 8번 (채팅방 수 무관)

        List<Long> chatRoomIds = chatRooms.stream()
                .map(ChatRoom::getChatRoomId)
                .collect(Collectors.toList());

        // 채팅방별 현재 인원 수 (IN 쿼리 1번)
        Map<Long, Long> participantCountMap = chatRoomMemberRepository.countByChatRoomIds(chatRoomIds)
                .stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));

        // 방장 닉네임 (IN 쿼리 1번)
        List<Long> hostIds = chatRooms.stream()
                .map(ChatRoom::getCreatedBy)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, String> hostNicknameMap = userRepository.findNicknamesByUserIds(hostIds)
                .stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (String) row[1]
                ));

        // 현재 사용자가 참여 중인 채팅방 ID (IN 쿼리 1번)
        java.util.Set<Long> joinedRoomIds = new java.util.HashSet<>(
                chatRoomMemberRepository.findJoinedRoomIdsByUserId(chatRoomIds, userId));

        // 현재 사용자가 강퇴된 채팅방 ID (IN 쿼리 1번)
        java.util.Set<Long> kickedRoomIds = new java.util.HashSet<>(
                chatRoomMemberRepository.findKickedRoomIdsByUserId(chatRoomIds, userId));

        // 지원서, AI 평가, 이력서, 다른 방 참여 여부 — userId+jobMasterId 기준으로 채팅방이 달라도 동일
        // stream 밖에서 1회만 조회하여 N번 반복 조회 제거
        Optional<JobApplication> jobApplicationOpt = jobApplicationRepository
                .findByUserIdAndJobMasterId(userId, jobMasterId);

        JobApplication jobApplication = jobApplicationOpt.orElse(null);

        AiApplicantEvaluation evaluation = null;
        boolean hasResume = false;
        Optional<ChatRoomMember> otherRoomMember = Optional.empty();

        if (jobApplication != null) {
            evaluation = aiApplicationEvaluationRepository
                    .findByJobApplicationId(jobApplication.getId())
                    .orElse(null);
            hasResume = applicationDocumentRepository
                    .existsByJobApplicationIdAndDocType(jobApplication.getId(), ApplicationDocumentType.RESUME);
            otherRoomMember = chatRoomMemberRepository
                    .findByJobApplicationIdAndNotKicked(jobApplication.getId());
        }

        ChatRoomPreloadedData preloadedData = new ChatRoomPreloadedData(
                participantCountMap,
                hostNicknameMap,
                joinedRoomIds,
                kickedRoomIds,
                jobApplication,
                evaluation,
                hasResume,
                otherRoomMember
        );

        List<ChatRoomSummaryResponse> summaries = chatRooms.stream()
                .map(room -> convertToSummary(room, userId, preloadedData))
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

        // 3. 생성자 서류 제출 확인 (이력서 필수, 포트폴리오 선택)
        validateDocumentsSubmitted(jobApplication.getId(), userId);

        // 4. 생성자 AI 점수 확인
        AiApplicantEvaluation evaluation = aiApplicationEvaluationRepository
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
        if (chatRoomRepository.existsByJobMasterIdAndCreatedByAndDeletedAtIsNullAndStatus(
                jobMasterId, userId, RoomStatus.ACTIVE)) {
            log.warn("이미 해당 공고에 활성 채팅방이 있습니다: jobMasterId={}, userId={}", jobMasterId, userId);
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

        // 9. 시스템 메시지 생성 ("채팅방이 생성되었습니다")
        ChatMessage systemMessage = ChatMessage.createSystemMessage(
                savedChatRoom.getChatRoomId(),
                "채팅방이 생성되었습니다."
        );
        chatMessageRepository.save(systemMessage);
        log.info("생성 시스템 메시지 생성 완료: chatRoomId={}", savedChatRoom.getChatRoomId());

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

        // 5. 방장 닉네임 조회
        String hostNickname = userRepository.findNicknameByUserId(chatRoom.getCreatedBy())
                .orElse("알 수 없음");

        // 5. 응답 생성
        ChatRoomDetailResponse response = ChatRoomDetailResponse.builder()
                .chatRoomId(chatRoom.getChatRoomId())
                .roomName(chatRoom.getRoomName())
                .roomGoal(chatRoom.getRoomGoal())
                .cutlineScore(chatRoom.getCutlineScore())
                .currentParticipants((int) memberCount)
                .maxParticipants(chatRoom.getMaxParticipants())
                .hostNickname(hostNickname)
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

        // 1. 채팅방 조회 + 비관적 락 획득 (SELECT FOR UPDATE)
        // 변경 이유: 기존 findByIdNotDeleted()는 락 없이 조회하므로
        //           count() → save() 사이 gap에서 두 트랜잭션이 동시에 정원 체크를 통과하는
        //           race condition이 발생했음 (k6 테스트로 재현 확인)
        //           findByIdWithLock()으로 변경하면 첫 번째 트랜잭션이 commit할 때까지
        //           두 번째 트랜잭션이 대기하므로 정원 체크가 항상 최신 값 기준으로 직렬화됨
        ChatRoom chatRoom = chatRoomRepository.findByIdWithLock(chatRoomId)
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

        // 8. 서류 제출 확인 (이력서 필수, 포트폴리오 선택)
        validateDocumentsSubmitted(jobApplication.getId(), userId);

        // 9. 커트라인 점수 확인
        AiApplicantEvaluation evaluation = aiApplicationEvaluationRepository
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

        // 11. 시스템 메시지 생성 ("OO님이 입장했습니다")
        String nickname = userRepository.findNicknameByUserId(userId)
                .orElse("사용자");
        ChatMessage systemMessage = ChatMessage.createSystemMessage(
                chatRoomId,
                nickname + "님이 입장했습니다."
        );
        chatMessageRepository.save(systemMessage);
        log.info("입장 시스템 메시지 생성 완료: chatRoomId={}, userId={}", chatRoomId, userId);
    }

    // 채팅방 종료 여부 확인 (Controller에서 SSE 이벤트 분기용)
    //
    // leaveChatRoom()이 void이므로 방장 자동 종료 여부를 Controller에서 알 수 없음.
    // 반환 타입 변경 없이 Service 시그니처를 유지하면서 Controller가 종료 여부를
    // 판단할 수 있도록 별도 조회 메서드를 제공.
    public boolean isRoomClosed(Long chatRoomId) {
        return chatRoomRepository.findByIdNotDeleted(chatRoomId)
                .map(room -> room.getStatus() == RoomStatus.CLOSED)
                .orElse(true); // 방이 없으면 종료된 것으로 간주
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

        // 3. 방장인 경우 채팅방 종료 처리
        // 수정 이유: 방장 입장에서 나가기와 종료하기는 동일한 결과이므로
        //           나가기 시도 시 에러를 던지는 대신 채팅방을 자동 종료
        if (member.getRole() == MemberRole.HOST) {
            log.info("방장의 나가기 시도 → 채팅방 자동 종료: chatRoomId={}, userId={}", chatRoomId, userId);

            // 종료 처리 전에 활성 멤버 조회 (chatRoom.close() 이후에도 kicked_at IS NULL 조건은 유지되므로
            // 순서 무관하지만, 의도를 명확히 하기 위해 종료 전에 조회)
            List<ChatRoomMember> activeMembers = chatRoomMemberRepository.findAllActiveMembersByChatRoomId(chatRoomId);

            chatRoom.close();
            chatRoomRepository.save(chatRoom);

            ChatMessage systemMessage = ChatMessage.createSystemMessage(
                    chatRoomId,
                    "채팅방이 종료되었습니다."
            );
            chatMessageRepository.save(systemMessage);
            log.info("방장 나가기로 인한 채팅방 종료 완료: chatRoomId={}", chatRoomId);

            // 방장 제외 활성 멤버 전원에게 종료 알림 발행
            activeMembers.stream()
                    .filter(m -> !m.getUserId().equals(userId))
                    .forEach(m -> eventPublisher.publishEvent(new ChatRoomNotificationEvent(
                            m.getUserId(),
                            "CHAT_ROOM_CLOSED",
                            chatRoom.getRoomName(),
                            chatRoomId
                    )));
            log.info("채팅방 종료 알림 이벤트 발행 완료 (방장 자동 종료): chatRoomId={}", chatRoomId);
            return;
        }

        // 4. 닉네임 조회 (삭제 전에 조회해야 함)
        String nickname = userRepository.findNicknameByUserId(userId)
                .orElse("사용자");

        // 5. 멤버 삭제
        chatRoomMemberRepository.delete(member);
        log.info("채팅방 퇴장 완료: chatRoomId={}, userId={}, chatRoomMemberId={}",
                chatRoomId, userId, member.getChatRoomMemberId());

        // 6. 시스템 메시지 생성 ("OO님이 퇴장했습니다")
        ChatMessage systemMessage = ChatMessage.createSystemMessage(
                chatRoomId,
                nickname + "님이 퇴장했습니다."
        );
        chatMessageRepository.save(systemMessage);
        log.info("퇴장 시스템 메시지 생성 완료: chatRoomId={}, userId={}", chatRoomId, userId);
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

        // 7. 닉네임 조회 (강퇴 전에 조회)
        String nickname = userRepository.findNicknameByUserId(targetMember.getUserId())
                .orElse("사용자");

        // 8. 강퇴 처리 (kicked_at 설정)
        targetMember.kick();
        chatRoomMemberRepository.save(targetMember);
        log.info("멤버 강퇴 완료: chatRoomId={}, kickedUserId={}", chatRoomId, targetMember.getUserId());

        // 9. 시스템 메시지 생성 ("OO님이 강제 퇴장되었습니다")
        ChatMessage systemMessage = ChatMessage.createSystemMessage(
                chatRoomId,
                nickname + "님이 강제 퇴장되었습니다."
        );
        chatMessageRepository.save(systemMessage);
        log.info("강퇴 시스템 메시지 생성 완료: chatRoomId={}, kickedUserId={}",
                chatRoomId, targetMember.getUserId());

        // 10. 강퇴된 사용자에게 알림 발행
        // @TransactionalEventListener(AFTER_COMMIT)로 커밋 이후 수신되므로
        // 트랜잭션 롤백 시 알림이 발송되지 않는다.
        String chatRoomName = chatRoomRepository.findByIdNotDeleted(chatRoomId)
                .map(ChatRoom::getRoomName)
                .orElse("채팅방");
        eventPublisher.publishEvent(new ChatRoomNotificationEvent(
                targetMember.getUserId(),
                "CHAT_ROOM_KICKED",
                chatRoomName,
                chatRoomId
        ));
        log.info("강퇴 알림 이벤트 발행: chatRoomId={}, kickedUserId={}", chatRoomId, targetMember.getUserId());
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
        // 종료 전에 활성 멤버 조회 (방장 제외 알림 발송용)
        List<ChatRoomMember> activeMembers = chatRoomMemberRepository.findAllActiveMembersByChatRoomId(chatRoomId);

        chatRoom.close();
        chatRoomRepository.save(chatRoom);
        log.info("채팅방 종료 완료: chatRoomId={}", chatRoomId);

        // 5. 시스템 메시지 생성 ("채팅방이 종료되었습니다")
        ChatMessage systemMessage = ChatMessage.createSystemMessage(
                chatRoomId,
                "채팅방이 종료되었습니다."
        );
        chatMessageRepository.save(systemMessage);
        log.info("종료 시스템 메시지 생성 완료: chatRoomId={}", chatRoomId);

        // 6. 방장 제외 활성 멤버 전원에게 종료 알림 발행
        activeMembers.stream()
                .filter(m -> !m.getUserId().equals(userId))
                .forEach(m -> eventPublisher.publishEvent(new ChatRoomNotificationEvent(
                        m.getUserId(),
                        "CHAT_ROOM_CLOSED",
                        chatRoom.getRoomName(),
                        chatRoomId
                )));
        log.info("채팅방 종료 알림 이벤트 발행 완료 (명시적 종료): chatRoomId={}", chatRoomId);
    }

    // 채팅방 멤버 목록 조회
    public ChatRoomMemberListResponse getChatRoomMembers(Long chatRoomId, Long userId) {
        log.info("채팅방 멤버 목록 조회 시작: chatRoomId={}, userId={}", chatRoomId, userId);

        // 1. 채팅방 존재 확인
        chatRoomRepository.findByIdNotDeleted(chatRoomId)
                .orElseThrow(() -> new ApiException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        // 2. 요청자가 채팅방 멤버인지 확인 (권한 검증)
        chatRoomMemberRepository.findByChatRoomIdAndUserIdAndKickedAtIsNull(chatRoomId, userId)
                .orElseThrow(() -> new ApiException(ErrorCode.CHAT_ROOM_ACCESS_DENIED));

        // 3. 활성 멤버 목록 조회 (HOST 우선, 입장 시간 오름차순)
        List<ChatRoomMember> members = chatRoomMemberRepository.findAllActiveMembersByChatRoomId(chatRoomId);

        // 4. 닉네임 + profileImageFileId 일괄 조회 후 DTO 변환
        // 개선: 기존 닉네임만 조회하던 방식에서 profileImageFileId 포함 조회로 변경
        //       유저 프로필 이미지 변경 시 채팅방 멤버 목록에 반영되지 않던 문제 해결
        List<Long> userIds = members.stream()
                .map(ChatRoomMember::getUserId)
                .collect(Collectors.toList());

        // userId → [nickname, profileImageFileId] 맵 구성 (IN 쿼리 1번)
        Map<Long, String> nicknameMap = new java.util.HashMap<>();
        Map<Long, Long> profileImageFileIdMap = new java.util.HashMap<>();

        userRepository.findNicknameAndProfileImageByUserIds(userIds)
                .forEach(row -> {
                    Long uid = (Long) row[0];
                    nicknameMap.put(uid, (String) row[1]);
                    profileImageFileIdMap.put(uid, (Long) row[2]);
                });

        // presigned URL 생성 (profileImageFileId가 있는 경우에만)
        // 만료 시간 10분: 멤버 목록은 자주 갱신되므로 짧게 설정
        Duration profileUrlExpiration = Duration.ofMinutes(10);
        Map<Long, String> profileImageUrlMap = new java.util.HashMap<>();
        profileImageFileIdMap.forEach((uid, fileId) -> {
            if (fileId != null) {
                try {
                    String url = s3FileManagementService.generatePresignedUrl(fileId, profileUrlExpiration);
                    profileImageUrlMap.put(uid, url);
                } catch (Exception e) {
                    // 파일이 삭제되었거나 S3 오류 시 해당 유저의 이미지는 null로 처리
                    log.warn("프로필 이미지 URL 생성 실패: userId={}, fileId={}", uid, fileId);
                }
            }
        });

        List<ChatRoomMemberResponse> memberResponses = members.stream()
                .map(member -> {
                    String nickname = nicknameMap.getOrDefault(member.getUserId(), "알 수 없음");
                    String profileImageUrl = profileImageUrlMap.get(member.getUserId());
                    return ChatRoomMemberResponse.of(member, nickname, profileImageUrl);
                })
                .collect(Collectors.toList());

        log.info("채팅방 멤버 목록 조회 완료: chatRoomId={}, memberCount={}", chatRoomId, memberResponses.size());

        return ChatRoomMemberListResponse.of(memberResponses);
    }
    // 채팅방 멤버 단건 조회
    // 추가 근거: GET /api/v1/chat-rooms/{chatRoomId}/members/{chatRoomMemberId} 핸들러 신규 구현에 따라
    //           목록 조회(getChatRoomMembers)와 동일한 권한 검증·presigned URL 생성 패턴을 단건에 적용
    //           chatRoomId 교차 검증을 포함해 다른 방 멤버를 조회하는 부정 접근 방어
    public ChatRoomMemberResponse getChatRoomMember(Long chatRoomId, Long requestUserId, Long chatRoomMemberId) {
        log.info("채팅방 멤버 단건 조회 시작: chatRoomId={}, requestUserId={}, chatRoomMemberId={}",
                chatRoomId, requestUserId, chatRoomMemberId);

        // 1. 채팅방 존재 확인
        chatRoomRepository.findByIdNotDeleted(chatRoomId)
                .orElseThrow(() -> new ApiException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        // 2. 요청자가 채팅방 멤버인지 확인 (권한 검증 — getChatRoomMembers와 동일한 패턴)
        chatRoomMemberRepository.findByChatRoomIdAndUserIdAndKickedAtIsNull(chatRoomId, requestUserId)
                .orElseThrow(() -> new ApiException(ErrorCode.CHAT_ROOM_ACCESS_DENIED));

        // 3. 대상 멤버 조회 (강퇴되지 않은 활성 멤버만)
        ChatRoomMember member = chatRoomMemberRepository
                .findByChatRoomMemberIdAndKickedAtIsNull(chatRoomMemberId)
                .orElseThrow(() -> new ApiException(ErrorCode.CHAT_MEMBER_NOT_FOUND));

        // 4. chatRoomId 교차 검증: 해당 멤버가 요청 chatRoom 소속인지 확인
        //    의도: /chat-rooms/1/members/2 호출 시 memberId=2가 chatRoom=99 소속이어도 조회 가능한
        //          보안 취약점을 차단
        if (!member.getChatRoomId().equals(chatRoomId)) {
            log.warn("chatRoomId 불일치: 요청 chatRoomId={}, 실제 chatRoomId={}, chatRoomMemberId={}",
                    chatRoomId, member.getChatRoomId(), chatRoomMemberId);
            throw new ApiException(ErrorCode.CHAT_MEMBER_NOT_FOUND);
        }

        // 5. 닉네임 + profileImageFileId 조회 (목록 조회와 동일 방식)
        List<Long> userIds = List.of(member.getUserId());
        Map<Long, String> nicknameMap = new java.util.HashMap<>();
        Map<Long, Long> profileImageFileIdMap = new java.util.HashMap<>();

        userRepository.findNicknameAndProfileImageByUserIds(userIds)
                .forEach(row -> {
                    Long uid = (Long) row[0];
                    nicknameMap.put(uid, (String) row[1]);
                    profileImageFileIdMap.put(uid, (Long) row[2]);
                });

        // 6. presigned URL 생성 (profileImageFileId가 있는 경우에만)
        //    만료 10분: 멤버 단건 조회도 목록과 동일한 만료 시간 정책 적용
        String profileImageUrl = null;
        Long fileId = profileImageFileIdMap.get(member.getUserId());
        if (fileId != null) {
            try {
                profileImageUrl = s3FileManagementService.generatePresignedUrl(fileId, Duration.ofMinutes(10));
            } catch (Exception e) {
                log.warn("프로필 이미지 URL 생성 실패: userId={}, fileId={}", member.getUserId(), fileId);
            }
        }

        String nickname = nicknameMap.getOrDefault(member.getUserId(), "알 수 없음");
        ChatRoomMemberResponse response = ChatRoomMemberResponse.of(member, nickname, profileImageUrl);

        log.info("채팅방 멤버 단건 조회 완료: chatRoomId={}, chatRoomMemberId={}", chatRoomId, chatRoomMemberId);
        return response;
    }

    // 내가 참여 중인 채팅방 목록 조회
    // 사용자는 본인이 참여 중인 채팅방 리스트를 확인할 수 있어야 함
    // 최신 참여 순으로 정렬하여 활동성 높은 채팅방을 우선 표시
    // 개선: stream 내 4번 반복 조회(4N+1) → chatRoomId 기반 IN 쿼리 4번 고정으로 대체
    public MyChatRoomListResponse getMyChatRooms(
            Long userId,
            Long cursor,
            int size
    ) {
        log.info("내 채팅방 목록 조회 시작: userId={}, cursor={}, size={}", userId, cursor, size);

        // 1. 내가 참여 중인 멤버십 조회 (최신 참여 순, size+1개 조회하여 hasNext 판단)
        PageRequest pageRequest = PageRequest.of(0, size + 1);
        List<ChatRoomMember> members = chatRoomMemberRepository.findMyChatRooms(
                userId,
                cursor,
                pageRequest
        );

        log.debug("조회된 멤버십 수: {}", members.size());

        // 2. hasNext 판단 및 실제 반환할 데이터 추출
        boolean hasNext = members.size() > size;
        List<ChatRoomMember> actualMembers = hasNext
                ? members.subList(0, size)
                : members;

        if (actualMembers.isEmpty()) {
            return MyChatRoomListResponse.builder()
                    .chatRooms(List.of())
                    .pagination(PaginationResponse.builder()
                            .nextCursor(null)
                            .hasNext(false)
                            .size(0)
                            .build())
                    .build();
        }

        // 3. chatRoomId 목록 추출 — 이후 4개 IN 쿼리의 공통 키
        List<Long> chatRoomIds = actualMembers.stream()
                .map(ChatRoomMember::getChatRoomId)
                .collect(Collectors.toList());

        // ── IN 쿼리 일괄 조회 (채팅방 수와 무관하게 각 1번씩, 총 4번) ──────────────

        // 3-1. 채팅방 정보 일괄 조회
        // 개선 근거: stream 내 findById() N번 → IN 쿼리 1번
        Map<Long, ChatRoom> chatRoomMap = chatRoomRepository
                .findAllByChatRoomIdIn(chatRoomIds)
                .stream()
                .collect(Collectors.toMap(ChatRoom::getChatRoomId, cr -> cr));

        // 3-2. 채팅방별 현재 인원 수 일괄 조회
        // 개선 근거: countByChatRoomIdAndKickedAtIsNull() N번 → IN 쿼리 1번 (8N+1 작업 시 추가된 메서드 재사용)
        Map<Long, Long> participantCountMap = chatRoomMemberRepository
                .countByChatRoomIds(chatRoomIds)
                .stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));

        // 3-3. 방장 닉네임 일괄 조회
        // 개선 근거: findNicknameByUserId() N번 → IN 쿼리 1번 (8N+1 작업 시 추가된 메서드 재사용)
        List<Long> hostIds = chatRoomMap.values().stream()
                .map(ChatRoom::getCreatedBy)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, String> hostNicknameMap = userRepository
                .findNicknamesByUserIds(hostIds)
                .stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (String) row[1]
                ));

        // 3-4. 채팅방별 마지막 메시지 일괄 조회
        // 개선 근거: findTopByChatRoomIdOrderBySentAtDesc() N번 → IN + 서브쿼리 1번
        Map<Long, ChatMessage> lastMessageMap = chatMessageRepository
                .findLastMessagesByChatRoomIds(chatRoomIds)
                .stream()
                .collect(Collectors.toMap(ChatMessage::getChatRoomId, msg -> msg));

        // ─────────────────────────────────────────────────────────────────────
        // 4. stream 내에서는 Map 조회만 수행 (DB 쿼리 0번)
        // 개선 근거: 기존에는 채팅방마다 4번 DB 조회 발생 → Map.get()으로 대체하여 쿼리 제거
        List<MyChatRoomResponse> chatRoomResponses = actualMembers.stream()
                .map(member -> {
                    ChatRoom chatRoom = chatRoomMap.get(member.getChatRoomId());
                    if (chatRoom == null) {
                        // 조회 시점과 삭제 시점 사이 race condition 방어 처리
                        log.warn("멤버십은 있으나 채팅방을 찾을 수 없음: chatRoomId={}", member.getChatRoomId());
                        return null;
                    }

                    long currentParticipants = participantCountMap
                            .getOrDefault(chatRoom.getChatRoomId(), 0L);

                    String hostNickname = hostNicknameMap
                            .getOrDefault(chatRoom.getCreatedBy(), "알 수 없음");

                    ChatMessage lastMessage = lastMessageMap.get(chatRoom.getChatRoomId());

                    String lastMessagePreview = Optional.ofNullable(lastMessage)
                            .map(msg -> {
                                if (msg.getMessageType().name().equals("FILE")) {
                                    return "[파일]";
                                } else if (msg.getMessageType().name().equals("SYSTEM")) {
                                    return msg.getContent();
                                } else {
                                    String content = msg.getContent();
                                    return content.length() > 50
                                            ? content.substring(0, 50) + "..."
                                            : content;
                                }
                            })
                            .orElse(null);

                    LocalDateTime lastMessageAt = Optional.ofNullable(lastMessage)
                            .map(ChatMessage::getSentAt)
                            .orElse(null);

                    return MyChatRoomResponse.builder()
                            .chatRoomId(chatRoom.getChatRoomId())
                            .jobMasterId(chatRoom.getJobMasterId())
                            .roomName(chatRoom.getRoomName())
                            .roomGoal(chatRoom.getRoomGoal())
                            .cutlineScore(chatRoom.getCutlineScore())
                            .currentParticipants((int) currentParticipants)
                            .maxParticipants(chatRoom.getMaxParticipants())
                            .hostNickname(hostNickname)
                            .preferredConditions(chatRoom.getPreferredConditions())
                            .status(chatRoom.getStatus())
                            .myRole(member.getRole())
                            .lastMessagePreview(lastMessagePreview)
                            .lastMessageAt(lastMessageAt)
                            .joinedAt(member.getJoinedAt())
                            .build();
                })
                .filter(response -> response != null)
                .collect(Collectors.toList());

        // 5. 다음 커서 계산 (마지막 멤버의 chatRoomMemberId)
        Long nextCursor = hasNext && !actualMembers.isEmpty()
                ? actualMembers.get(actualMembers.size() - 1).getChatRoomMemberId()
                : null;

        // 6. 페이징 정보 생성
        PaginationResponse pagination = PaginationResponse.builder()
                .nextCursor(nextCursor)
                .hasNext(hasNext)
                .size(chatRoomResponses.size())
                .build();

        log.info("내 채팅방 목록 조회 완료: userId={}, 조회된 방 수={}, hasNext={}",
                userId, chatRoomResponses.size(), hasNext);

        return MyChatRoomListResponse.builder()
                .chatRooms(chatRoomResponses)
                .pagination(pagination)
                .build();
    }
}