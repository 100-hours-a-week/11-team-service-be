package com.thunder11.scuad.chat.service;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
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
import com.thunder11.scuad.infra.rabbitmq.config.RabbitMQConfig;
import com.thunder11.scuad.infra.rabbitmq.dto.AiRequestMessage;
import com.thunder11.scuad.jobposting.domain.AiEvalJob;
import com.thunder11.scuad.jobposting.domain.JobApplication;
import com.thunder11.scuad.jobposting.domain.type.AiJobStatus;
import com.thunder11.scuad.jobposting.domain.type.AnalysisType;
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
    private final RabbitTemplate rabbitTemplate;

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

        // 8. AiEvalJob 생성
        //    COMPARISON 타입은 콜백 수신 시 my + competitor 두 지원 ID가 모두 필요하므로
        //    competitorApplication을 함께 저장한다
        AiEvalJob evalJob = AiEvalJob.builder()
                .jobApplication(myApplication)
                .competitorApplication(competitorApplication)
                .requestedBy(requestUser)
                .analysisType(AnalysisType.COMPARISON)
                .status(AiJobStatus.PENDING)
                .build();
        AiEvalJob savedJob = aiEvalJobRepository.save(evalJob);
        savedJob.startProcessing();
        aiEvalJobRepository.save(savedJob);

        // 9. RabbitMQ 메시지 발행
        //    AI 호출을 큐에 위임하고 즉시 반환하여 요청 스레드를 해제한다
        AiRequestMessage message = AiRequestMessage.builder()
                .evalJobId(String.valueOf(savedJob.getId()))
                .userId(String.valueOf(myApplication.getUser().getUserId()))
                .jobPostingId(String.valueOf(myApplication.getJobMaster().getId()))
                .competitor(String.valueOf(competitorApplication.getUser().getUserId()))
                .build();

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_NAME,
                RabbitMQConfig.QUEUE_COMPARISON,
                message
        );
        log.info("RabbitMQ 발송 완료: evalJobId={}, queue={}",
                savedJob.getId(), RabbitMQConfig.QUEUE_COMPARISON);
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

        // 6. 비교 결과 조회
        //    AI 처리가 완료되지 않은 경우 404 반환 → 클라이언트 폴링 유도
        return aiApplicantComparisonRepository
                .findByMyApplication_IdAndCompetitorApplication_Id(
                        myApplication.getId(),
                        competitorApplication.getId()
                )
                .map(ComparisonResponse::from)
                .orElseThrow(() -> new ApiException(ErrorCode.COMPARISON_RESULT_NOT_FOUND));
    }
}
