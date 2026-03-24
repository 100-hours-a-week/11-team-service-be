package com.thunder11.scuad.chat.service;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.thunder11.scuad.auth.domain.User;
import com.thunder11.scuad.auth.repository.UserRepository;
import com.thunder11.scuad.chat.domain.ChatRoomMember;
import com.thunder11.scuad.chat.dto.response.ComparisonResponse;
import com.thunder11.scuad.chat.repository.ChatRoomMemberRepository;
import com.thunder11.scuad.chat.repository.ChatRoomRepository;
import com.thunder11.scuad.common.exception.ApiException;
import com.thunder11.scuad.common.exception.ErrorCode;
import com.thunder11.scuad.jobposting.domain.AiEvalJob;
import com.thunder11.scuad.jobposting.domain.JobApplication;
import com.thunder11.scuad.jobposting.domain.type.AiJobStatus;
import com.thunder11.scuad.jobposting.domain.type.AnalysisType;
import com.thunder11.scuad.jobposting.event.AiComparisonCreateEvent;
import com.thunder11.scuad.jobposting.repository.AiApplicantComparisonRepository;
import com.thunder11.scuad.jobposting.repository.AiEvalJobRepository;
import com.thunder11.scuad.jobposting.repository.JobApplicationRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMemberComparisonService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final AiApplicantComparisonRepository aiApplicantComparisonRepository;
    private final AiEvalJobRepository aiEvalJobRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * AI 비교 분석 요청 — 비동기 (POST 전용)
     *
     * 기존 동기 구조에서는 AI 호출 응답까지 스레드를 점유했으나,
     * 비동기 전환 후에는 AiEvalJob 생성 + RabbitMQ 메시지 발행만 수행하고 즉시 반환한다.
     * 실제 AI 처리 및 DB 저장은 AI 서버가 큐를 소비한 뒤 /api/internal/ai/callback으로
     * 결과를 전달하면 AiResultProcessingService가 처리한다.
     */
    @Transactional
    public void requestComparison(Long chatRoomId, Long requestUserId, Long chatRoomMemberId) {
        log.info("비교 분석 요청: chatRoomId={}, requestUserId={}, targetMemberId={}",
                chatRoomId, requestUserId, chatRoomMemberId);

        // 1. 채팅방 존재 확인
        chatRoomRepository.findByIdNotDeleted(chatRoomId)
                .orElseThrow(() -> new ApiException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        // 2. 요청자가 채팅방 멤버인지 확인
        ChatRoomMember myMember = chatRoomMemberRepository
                .findByChatRoomIdAndUserIdAndKickedAtIsNull(chatRoomId, requestUserId)
                .orElseThrow(() -> new ApiException(ErrorCode.CHAT_ROOM_ACCESS_DENIED));

        // 3. 비교 대상 멤버 조회
        ChatRoomMember targetMember = chatRoomMemberRepository
                .findByChatRoomMemberIdAndKickedAtIsNull(chatRoomMemberId)
                .orElseThrow(() -> new ApiException(ErrorCode.CHAT_MEMBER_NOT_FOUND));

        // 4. 대상 멤버가 같은 채팅방 소속인지 검증
        if (!targetMember.getChatRoomId().equals(chatRoomId)) {
            throw new ApiException(ErrorCode.CHAT_MEMBER_NOT_FOUND);
        }

        // 5. 자기 자신과 비교 방지
        if (myMember.getChatRoomMemberId().equals(chatRoomMemberId)) {
            throw new ApiException(ErrorCode.COMPARISON_SELF_NOT_ALLOWED);
        }

        // 6. 나와 대상 멤버의 지원서 조회
        JobApplication myApplication = jobApplicationRepository
                .findById(myMember.getJobApplicationId())
                .orElseThrow(() -> new ApiException(ErrorCode.JOB_APPLICATION_NOT_FOUND));

        JobApplication competitorApplication = jobApplicationRepository
                .findById(targetMember.getJobApplicationId())
                .orElseThrow(() -> new ApiException(ErrorCode.JOB_APPLICATION_NOT_FOUND));

        // 7. 요청자 User 조회 (AiEvalJob.requestedBy 필드용)
        User requestUser = userRepository.findById(requestUserId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        // 8. 중복 요청 방지
        //    이미 PROCESSING 중인 비교 작업이 있으면 중복 요청을 막는다.
        //    사용자가 버튼을 여러 번 누르는 경우 AiEvalJob이 중복 생성되는 것을 방지하기 위함.
        aiEvalJobRepository.findFirstByJobApplicationIdAndAnalysisTypeOrderByIdDesc(
                myApplication.getId(), AnalysisType.COMPARISON)
                .ifPresent(existingJob -> {
                    if (existingJob.getStatus() == AiJobStatus.PROCESSING) {
                        throw new ApiException(ErrorCode.CONFLICT, "이미 진행 중인 비교 분석이 있습니다.");
                    }
                });

        // 9. AiEvalJob 생성
        //    COMPARISON 타입은 콜백 수신 시 my + competitor 두 지원 ID가 모두 필요하므로
        //    competitorApplication을 함께 저장한다.
        //    다른 타입과 동일하게 PROCESSING 상태로 바로 생성하여 불필요한 save 호출을 줄인다.
        AiEvalJob evalJob = AiEvalJob.builder()
                .jobApplication(myApplication)
                .competitorApplication(competitorApplication)
                .requestedBy(requestUser)
                .analysisType(AnalysisType.COMPARISON)
                .status(AiJobStatus.PROCESSING)
                .build();
        AiEvalJob savedJob = aiEvalJobRepository.save(evalJob);

        // 9. 이벤트 발행
        //    트랜잭션 커밋 후 AiEvaluationWorker의 @TransactionalEventListener(AFTER_COMMIT)가 수신하여 MQ 발행.
        //    트랜잭션 내부에서 직접 MQ를 발행하면 DB 롤백 시에도 메시지가 발행되는 문제가 있으므로
        //    커밋 확정 후에 발행을 위임하는 방식을 사용한다.
        eventPublisher.publishEvent(new AiComparisonCreateEvent(
                savedJob.getId(),
                myApplication.getUser().getUserId(),
                myApplication.getJobMaster().getId(),
                competitorApplication.getUser().getUserId()
        ));
        log.info("비교 분석 이벤트 발행 완료: evalJobId={}", savedJob.getId());
    }

    /**
     * AI 비교 분석 결과 조회 — GET 전용
     *
     * 비동기 전환 후 이 메서드는 결과 조회만 담당한다.
     * AI 처리가 아직 완료되지 않아 DB에 결과가 없으면 COMPARISON_RESULT_NOT_FOUND(404)를 반환하며,
     * 클라이언트는 이 응답을 받으면 잠시 후 재조회(폴링)하면 된다.
     */
    @Transactional(readOnly = true)
    public ComparisonResponse getComparisonResult(Long chatRoomId, Long requestUserId, Long chatRoomMemberId) {
        log.info("비교 결과 조회: chatRoomId={}, requestUserId={}, targetMemberId={}",
                chatRoomId, requestUserId, chatRoomMemberId);

        // 1. 채팅방 존재 확인
        chatRoomRepository.findByIdNotDeleted(chatRoomId)
                .orElseThrow(() -> new ApiException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        // 2. 요청자가 채팅방 멤버인지 확인
        ChatRoomMember myMember = chatRoomMemberRepository
                .findByChatRoomIdAndUserIdAndKickedAtIsNull(chatRoomId, requestUserId)
                .orElseThrow(() -> new ApiException(ErrorCode.CHAT_ROOM_ACCESS_DENIED));

        // 3. 비교 대상 멤버 조회
        ChatRoomMember targetMember = chatRoomMemberRepository
                .findByChatRoomMemberIdAndKickedAtIsNull(chatRoomMemberId)
                .orElseThrow(() -> new ApiException(ErrorCode.CHAT_MEMBER_NOT_FOUND));

        // 4. 대상 멤버가 같은 채팅방 소속인지 검증
        if (!targetMember.getChatRoomId().equals(chatRoomId)) {
            throw new ApiException(ErrorCode.CHAT_MEMBER_NOT_FOUND);
        }

        // 5. 지원서 조회
        JobApplication myApplication = jobApplicationRepository
                .findById(myMember.getJobApplicationId())
                .orElseThrow(() -> new ApiException(ErrorCode.JOB_APPLICATION_NOT_FOUND));

        JobApplication competitorApplication = jobApplicationRepository
                .findById(targetMember.getJobApplicationId())
                .orElseThrow(() -> new ApiException(ErrorCode.JOB_APPLICATION_NOT_FOUND));

        // 6. AiEvalJob 상태 확인 후 분기
        //    무조건 404를 반환하던 기존 방식 대신, AiEvalJob 상태에 따라 정확한 응답을 반환한다.
        //    - PENDING / PROCESSING: AI가 처리 중 → 202
        //    - FAILED: AI 처리 실패 → 500 (실패 사유 포함)
        //    - SUCCEEDED: DB에서 결과 조회
        //    - AiEvalJob 자체가 없음: 비교 요청을 아직 하지 않은 상태 → 404
        AiEvalJob recentJob = aiEvalJobRepository
                .findFirstByJobApplicationIdAndAnalysisTypeOrderByIdDesc(
                        myApplication.getId(), AnalysisType.COMPARISON)
                .orElse(null);

        if (recentJob != null) {
            switch (recentJob.getStatus()) {
                case PENDING, PROCESSING ->
                        throw new ApiException(ErrorCode.ACCEPTED, "AI가 비교 분석 중입니다. 잠시 후 다시 조회해주세요.");
                case FAILED ->
                        throw new ApiException(ErrorCode.INTERNAL_ERROR,
                                "비교 분석 중 오류가 발생했습니다: " + recentJob.getErrorMessage());
                case SUCCEEDED -> {}
            }
        }

        // 7. 비교 결과 조회
        return aiApplicantComparisonRepository
                .findByMyApplication_IdAndCompetitorApplication_Id(
                        myApplication.getId(),
                        competitorApplication.getId()
                )
                .map(ComparisonResponse::from)
                .orElseThrow(() -> {
                    // SUCCEEDED 상태인데 결과가 없는 경우는 비정상 케이스
                    // 콜백은 수신됐지만 저장 과정에서 문제가 생긴 상황이므로 500으로 처리
                    if (recentJob != null && recentJob.getStatus() == AiJobStatus.SUCCEEDED) {
                        return new ApiException(ErrorCode.INTERNAL_ERROR,
                                "비교 분석은 완료되었으나 결과 데이터가 없습니다.");
                    }
                    return new ApiException(ErrorCode.COMPARISON_RESULT_NOT_FOUND);
                });
    }
}
