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
import com.thunder11.scuad.jobposting.event.AiAnalysisCreateEvent;
import com.thunder11.scuad.jobposting.dto.response.AiPortfolioAnalysisResponse;
import com.thunder11.scuad.jobposting.dto.response.AiResumeAnalysisResponse;
import com.thunder11.scuad.jobposting.repository.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class JopApplicationAnalysisService {

    private final JobApplicationRepository jobApplicationRepository;
    private final AiEvalJobRepository aiEvalJobRepository;
    private final AiResumeAnalysisRepository aiResumeAnalysisRepository;
    private final AiPortfolioAnalysisRepository aiPortfolioAnalysisRepository;
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

        AiEvalJob recentJob = aiEvalJobRepository
                .findFirstByJobApplicationIdAndAnalysisTypeOrderByIdDesc(applicationId, AnalysisType.EVALUATION)
                .orElse(null);

        if (recentJob != null) {
            switch (recentJob.getStatus()) {
                case PENDING:
                case PROCESSING:
                    throw new ApiException(ErrorCode.ACCEPTED, "AI가 현재 이력서를 분석 중입니다.");
                case FAILED:
                    throw new ApiException(ErrorCode.INTERNAL_ERROR, "분석 중 오류가 발생했습니다: " + recentJob.getErrorMessage());
                case SUCCEEDED:
                    break;
            }
        }

        Optional<AiApplicantEvaluation> evaluationIsReady = aiApplicationEvaluationRepository
                .findByJobApplicationId(applicationId);

        if (evaluationIsReady.isPresent()) {
            return AiEvaluationResultResponse.from(evaluationIsReady.get());
        }

        if (recentJob != null && recentJob.getStatus() == AiJobStatus.SUCCEEDED) {
            throw new ApiException(ErrorCode.INTERNAL_ERROR, "분석은 완료되었으나 결과 데이터가 없습니다.");
        }

        throw new ApiException(ErrorCode.ACCEPTED, "AI 분석 대기 중입니다.");
    }

    @Transactional(readOnly = true)
    public AiResumeAnalysisResponse getResumeAnalysisResult(Long userId, Long applicationId) {
        JobApplication jobApplication = jobApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "지원서를 찾을 수 없습니다."));

        if (!jobApplication.getUser().getUserId().equals(userId)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "본인의 분석 결과만 조회할 수 있습니다.");
        }
        AiEvalJob recentJob = aiEvalJobRepository
                .findFirstByJobApplicationIdAndAnalysisTypeOrderByIdDesc(
                        applicationId, AnalysisType.RESUME)
                .orElse(null);
        if (recentJob != null) {
            switch (recentJob.getStatus()) {
                case PENDING, PROCESSING -> throw new ApiException(ErrorCode.ACCEPTED, "AI가 이력서를 분석 중입니다.");
                case FAILED -> throw new ApiException(ErrorCode.INTERNAL_ERROR, "분석 중 오류: " + recentJob.getErrorMessage());
                case SUCCEEDED -> {}
            }
        }
        return aiResumeAnalysisRepository.findByJobApplicationId(applicationId)
                .map(AiResumeAnalysisResponse::from)
                .orElseThrow(() -> new ApiException(ErrorCode.ACCEPTED, "AI 이력서 분석 대기 중입니다."));
    }

    @Transactional(readOnly = true)
    public AiPortfolioAnalysisResponse getPortfolioAnalysisResult(Long userId, Long applicationId) {
        JobApplication jobApplication = jobApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "지원서를 찾을 수 없습니다."));

        if (!jobApplication.getUser().getUserId().equals(userId)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "본인의 분석 결과만 조회할 수 있습니다.");
        }

        AiEvalJob recentJob = aiEvalJobRepository
                .findFirstByJobApplicationIdAndAnalysisTypeOrderByIdDesc(applicationId, AnalysisType.PORTFOLIO)
                .orElse(null);

        if (recentJob != null) {
            switch (recentJob.getStatus()) {
                case PENDING, PROCESSING -> throw new ApiException(ErrorCode.ACCEPTED, "AI가 포트폴리오 분석 중입니다.");
                case FAILED -> throw new ApiException(ErrorCode.INTERNAL_ERROR, "분석 중 오류: " + recentJob.getErrorMessage());
                case SUCCEEDED ->  {}
            }
        }

        return aiPortfolioAnalysisRepository.findByJobApplicationId(applicationId)
                .map(AiPortfolioAnalysisResponse::from)
                .orElseThrow(() -> new ApiException(ErrorCode.ACCEPTED, "AI 포트폴리오 분석 대기중입니다."));
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

        if (analysisType == AnalysisType.ALL) {
            boolean hasPortfolio = jobApplication.getApplicationDocuments().stream()
                    .anyMatch(d -> d.getDocType() == ApplicationDocumentType.PORTFOLIO);

            createJobOnlyWithPending(jobApplication, userId, AnalysisType.EVALUATION);
            createJobOnlyWithPending(jobApplication, userId, AnalysisType.RESUME);
            if(hasPortfolio) {
                createJobOnlyWithPending(jobApplication, userId, AnalysisType.PORTFOLIO);
            }
            eventPublisher.publishEvent(new AiAnalysisCreateEvent(userId, jobApplication.getJobMaster().getId(), AnalysisType.ALL));
            return null;
        }

        aiEvalJobRepository.findFirstByJobApplicationIdAndAnalysisTypeOrderByIdDesc(applicationId, analysisType)
                .ifPresent(aiEvalJob -> {
                    if (aiEvalJob.getStatus() == AiJobStatus.PROCESSING) {
                        throw new ApiException(ErrorCode.CONFLICT, "이미 진행 중인 평가가 있습니다.");
                    }
                });
        return createAndPublish(jobApplication, userId, analysisType);
    }

    private Long createAndPublish(JobApplication jobApplication, Long userId, AnalysisType analysisType) {
        AiEvalJob aiEvalJob = AiEvalJob.builder()
                .jobApplication(jobApplication)
                .requestedBy(jobApplication.getUser())
                .analysisType(analysisType)
                .status(AiJobStatus.PROCESSING)
                .build();

        AiEvalJob savedAiEvalJob = aiEvalJobRepository.save(aiEvalJob);
        log.info("AiEvalJob 저장 완료: ID={}, ApplicationId={}, Status={}",
                savedAiEvalJob.getId(), jobApplication.getId(), savedAiEvalJob.getStatus());

        eventPublisher.publishEvent(new AiAnalysisCreateEvent(userId, jobApplication.getJobMaster().getId(), analysisType));

        return savedAiEvalJob.getId();
    }

    private Long createJobOnlyWithPending(JobApplication jobApplication, Long userId, AnalysisType analysisType) {
        AiEvalJob aiEvalJob = AiEvalJob.builder()
                .jobApplication(jobApplication)
                .requestedBy(jobApplication.getUser())
                .analysisType(analysisType)
                .status(AiJobStatus.PENDING)
                .build();

        AiEvalJob savedAiEvalJob = aiEvalJobRepository.save(aiEvalJob);
        log.info("AI 평가 접수 완료(PENDING/이벤트 미발행): ID={}, Type={}", savedAiEvalJob.getId(), analysisType);

        return savedAiEvalJob.getId();
    }
}
