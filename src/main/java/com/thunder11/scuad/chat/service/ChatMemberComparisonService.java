package com.thunder11.scuad.chat.service;

import java.util.List;
import java.util.stream.Collectors;

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
import com.thunder11.scuad.infra.ai.client.AiComparePort;
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
    private final AiComparePort aiComparePort;

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

        AiCompareResponse aiResponse = aiComparePort.compareApplicants(aiRequest);

        // AI 응답을 도메인 객체로 변환
        List<ComparisonMetric> metrics = aiResponse.getComparisonMetrics().stream()
                .map(m -> new ComparisonMetric(m.getName(), m.getMyScore(), m.getCompetitorScore()))
                .collect(Collectors.toList());

        // DB 저장
        AiApplicantComparison comparison = AiApplicantComparison.builder()
                .jobMaster(jobMaster)
                .myApplication(myApplication)
                .competitorApplication(competitorApplication)
                .comparisonMetrics(metrics)
                .strengthsReport(aiResponse.getStrengthsReport())
                .weaknessesReport(aiResponse.getWeaknessesReport())
                .build();

        AiApplicantComparison saved = aiApplicantComparisonRepository.save(comparison);
        log.info("AI 비교 결과 저장 완료: comparisonId={}", saved.getId());

        return ComparisonResponse.from(saved);
    }
}