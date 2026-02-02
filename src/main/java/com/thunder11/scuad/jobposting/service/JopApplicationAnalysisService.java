package com.thunder11.scuad.jobposting.service;

import java.util.Optional;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.thunder11.scuad.common.exception.ApiException;
import com.thunder11.scuad.common.exception.ErrorCode;
import com.thunder11.scuad.jobposting.domain.AiApplicantEvaluation;
import com.thunder11.scuad.jobposting.domain.AiEvalJob;
import com.thunder11.scuad.jobposting.domain.JobApplication;
import com.thunder11.scuad.jobposting.domain.type.AiJobStatus;
import com.thunder11.scuad.jobposting.domain.type.AnalysisType;
import com.thunder11.scuad.jobposting.domain.type.ApplicationDocumentType;
import com.thunder11.scuad.jobposting.dto.response.AiEvaluationResultResponse;
import com.thunder11.scuad.jobposting.event.AiEvaluationCreateEvent;
import com.thunder11.scuad.jobposting.repository.AiApplicationEvaluationRepository;
import com.thunder11.scuad.jobposting.repository.AiEvalJobRepository;
import com.thunder11.scuad.jobposting.repository.JobApplicationRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class JopApplicationAnalysisService {

    private final JobApplicationRepository jobApplicationRepository;
    private final AiEvalJobRepository aiEvalJobRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final AiApplicationEvaluationRepository aiApplicationEvaluationRepository;

    @Transactional(readOnly = true)
    public AiEvaluationResultResponse getMyApplicationResult(Long userId, Long jobPostingId) {
        JobApplication jobApplication = jobApplicationRepository.findByUserUserIdAndJobMasterId(userId, jobPostingId)
                .orElse(null);

        if (jobApplication == null) {
            return null;
        }

        return getAnalysisResult(userId, jobApplication.getId());
    }

    @Transactional(readOnly = true)
    public AiEvaluationResultResponse getAnalysisResult(Long userId, Long applicationId) {
        JobApplication jobApplication = jobApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "지원서를 찾을 수 없습니다."));

        if (!jobApplication.getUser().getUserId().equals(userId)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "본인의 분석 결과만 조회할 수 있습니다.");
        }

        Optional<AiApplicantEvaluation> evaluationIsReady = aiApplicationEvaluationRepository
                .findByJobApplicationId(applicationId);

        if (evaluationIsReady.isPresent()) {
            return AiEvaluationResultResponse.from(evaluationIsReady.get());
        }

        AiEvalJob recentJob = aiEvalJobRepository
                .findFirstByJobApplicationIdAndAnalysisTypeOrderByIdDesc(applicationId, AnalysisType.EVALUATION)
                .orElse(null);

        if (recentJob == null) {
            throw new ApiException(ErrorCode.ACCEPTED, "AI 분석 대기 중입니다.");
        }

        switch (recentJob.getStatus()) {
            case PENDING:
            case PROCESSING:
                throw new ApiException(ErrorCode.ACCEPTED, "AI가 현재 이력서를 분석 중입니다.");
            case FAILED:
                throw new ApiException(ErrorCode.INTERNAL_ERROR, "분석 중 오류가 발생했습니다: " + recentJob.getErrorMessage());
            case SUCCEEDED:
                throw new ApiException(ErrorCode.INTERNAL_ERROR, "분석은 완료되었으나 결과 데이터가 없습니다.");
            default:
                throw new ApiException(ErrorCode.NOT_FOUND, "결과를 찾을 수 없습니다.");
        }
    }

    @Transactional
    public Long createEvaluationJob(Long applicationId, Long userId, String type) {
        AnalysisType analysisType;
        try {
            analysisType = AnalysisType.valueOf(type);
        } catch (IllegalArgumentException e) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "지원하지 않는 분석 타입입니다.");
        }

        JobApplication jobApplication = jobApplicationRepository.findByIdWithDocuments(applicationId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "지원서를 찾을 수 없습니다."));

        if (!jobApplication.getUser().getUserId().equals(userId)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "본인의 지원서만 평가를 요청할 수 있습니다.");
        }

        boolean hasResume = jobApplication.getApplicationDocuments() != null
                && jobApplication.getApplicationDocuments().stream()
                        .anyMatch(d -> d.getDocType() == ApplicationDocumentType.RESUME);
        if (!hasResume) {
            throw new ApiException(ErrorCode.NOT_FOUND, "이력서가 없습니다.");
        }

        aiEvalJobRepository.findFirstByJobApplicationIdAndAnalysisTypeOrderByIdDesc(applicationId, analysisType)
                .ifPresent(aiEvalJob -> {
                    if (aiEvalJob.getStatus() == AiJobStatus.PROCESSING) {
                        throw new ApiException(ErrorCode.CONFLICT, "이미 진행 중인 평가가 있습니다.");
                    }
                });

        AiEvalJob aiEvalJob = AiEvalJob.builder()
                .jobApplication(jobApplication)
                .requestedBy(jobApplication.getUser())
                .analysisType(AnalysisType.EVALUATION)
                .status(AiJobStatus.PROCESSING)
                .build();

        AiEvalJob savedAiEvalJob = aiEvalJobRepository.save(aiEvalJob);
        log.info("AiEvalJob 저장 완료: ID={}, ApplicationId={}, Status={}",
                savedAiEvalJob.getId(), applicationId, savedAiEvalJob.getStatus());

        eventPublisher.publishEvent(new AiEvaluationCreateEvent(userId, jobApplication.getJobMaster().getId()));

        return savedAiEvalJob.getId();
    }
}
