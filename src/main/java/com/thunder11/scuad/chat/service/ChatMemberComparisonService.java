package com.thunder11.scuad.chat.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.thunder11.scuad.chat.domain.ChatRoomMember;
import com.thunder11.scuad.chat.dto.response.ComparisonResponse;
import com.thunder11.scuad.chat.repository.ChatRoomMemberRepository;
import com.thunder11.scuad.chat.repository.ChatRoomRepository;
import com.thunder11.scuad.common.exception.ApiException;
import com.thunder11.scuad.common.exception.ErrorCode;
import com.thunder11.scuad.infra.ai.client.AiServiceClient;
import com.thunder11.scuad.infra.ai.dto.request.AiCompareRequest;
import com.thunder11.scuad.infra.ai.dto.response.AiCompareResponse;
import com.thunder11.scuad.jobposting.domain.AiApplicantComparison;
import com.thunder11.scuad.jobposting.domain.ComparisonMetric;
import com.thunder11.scuad.jobposting.domain.JobApplication;
import com.thunder11.scuad.jobposting.domain.JobMaster;
import com.thunder11.scuad.jobposting.repository.AiApplicantComparisonRepository;
import com.thunder11.scuad.jobposting.repository.JobApplicationRepository;

// 채팅방 멤버 간 AI 비교 분석 서비스
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMemberComparisonService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final AiApplicantComparisonRepository aiApplicantComparisonRepository;
    private final AiServiceClient aiServiceClient;
    private final AiComparisonSaveHelper aiComparisonSaveHelper;

    @Transactional
    public ComparisonResponse compare(Long chatRoomId, Long requestUserId, Long chatRoomMemberId) {
        log.info("비교 요청: chatRoomId={}, requestUserId={}, targetMemberId={}",
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

        // 7. DB에 기존 비교 결과 있으면 바로 반환 (AI 중복 호출 방지)
        return aiApplicantComparisonRepository
                .findByMyApplication_IdAndCompetitorApplication_Id(
                        myApplication.getId(),
                        competitorApplication.getId()
                )
                .map(ComparisonResponse::from)
                .orElseGet(() -> callAiAndSave(myApplication, competitorApplication));
    }

    // AI 비교 API 호출 후 결과 저장
    private ComparisonResponse callAiAndSave(
            JobApplication myApplication,
            JobApplication competitorApplication
    ) {
        JobMaster jobMaster = myApplication.getJobMaster();

        // AI 서버에 비교 요청
        AiCompareRequest aiRequest = AiCompareRequest.builder()
                .jobPostingId(String.valueOf(jobMaster.getId()))
                .userId(String.valueOf(myApplication.getUser().getUserId()))
                .competitor(String.valueOf(competitorApplication.getUser().getUserId()))
                .build();

        AiCompareResponse aiResponse = aiServiceClient.compareApplicants(aiRequest);

        // AI 응답을 도메인 객체로 변환
        List<ComparisonMetric> metrics = aiResponse.getComparisonMetrics().stream()
                .map(m -> new ComparisonMetric(m.getName(), m.getMyScore(), m.getCompetitorScore()))
                .collect(Collectors.toList());

        AiApplicantComparison comparison = AiApplicantComparison.builder()
                .jobMaster(jobMaster)
                .myApplication(myApplication)
                .competitorApplication(competitorApplication)
                .comparisonMetrics(metrics)
                .strengthsReport(aiResponse.getStrengthsReport())
                .weaknessesReport(aiResponse.getWeaknessesReport())
                .build();

        // race condition 대비: UNIQUE 제약 위반 시 이미 저장된 결과를 조회해서 반환
        // REQUIRES_NEW 독립 트랜잭션으로 분리한 이유:
        //   같은 트랜잭션 안에서 예외 발생 시 JPA 세션이 오염(rollback-only)되어
        //   catch 후 findBy 시도 시 AssertionFailure 발생 → 이슈11과 동일한 문제
        //   REQUIRES_NEW로 분리하면 예외 발생 시 내부 트랜잭션만 롤백되고
        //   외부 세션은 깨끗하게 유지되어 findBy가 정상 동작함
        try {
            AiApplicantComparison saved = aiComparisonSaveHelper.trySave(comparison);
            return ComparisonResponse.from(saved);
        } catch (DataIntegrityViolationException e) {
            log.warn("AI 비교 결과 중복 저장 감지, 기존 결과 반환: myApplicationId={}, competitorApplicationId={}",
                    myApplication.getId(), competitorApplication.getId());
            return aiApplicantComparisonRepository
                    .findByMyApplication_IdAndCompetitorApplication_Id(
                            myApplication.getId(), competitorApplication.getId())
                    .map(ComparisonResponse::from)
                    .orElseThrow(() -> new ApiException(ErrorCode.INTERNAL_ERROR));
        }
    }
}