package com.thunder11.scuad.jobposting.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.thunder11.scuad.infra.ai.client.AiServiceClient;
import com.thunder11.scuad.infra.ai.dto.request.AiEvaluationAnalysisRequest;
import com.thunder11.scuad.infra.ai.dto.response.AiEvaluationResultResponse;
import com.thunder11.scuad.jobposting.event.AiAnalysisCreateEvent;
import com.thunder11.scuad.infra.ai.dto.response.AiPortfolioAnalysisResponse;
import com.thunder11.scuad.infra.ai.dto.response.AiResumeAnalysisResponse;
import com.thunder11.scuad.jobposting.domain.*;
import com.thunder11.scuad.jobposting.repository.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiEvaluationWorker {

    private final AiEvalJobRepository aiEvalJobRepository;
    private final AiApplicationEvaluationRepository aiApplicationEvaluationRepository;
    private final AiResumeAnalysisRepository aiResumeAnalysisRepository;
    private final AiPortfolioAnalysisRepository aiPortfolioAnalysisRepository;
    private final JobMasterRepository jobMasterRepository;
    private final AiServiceClient aiServiceClient;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void processEvaluationAsync(AiAnalysisCreateEvent event) {
        log.info("AI 분석 작업시작: UserId={}, JobPostingId={}, Type={}",
                        event.getUserId(), event.getJobPostingId(), event.getAnalysisType());

        AiEvalJob aiEvalJob = aiEvalJobRepository
                        .findFirstByRequestedByUserIdAndJobApplicationJobMasterIdAndAnalysisTypeOrderByIdDesc(
                                        event.getUserId(),
                                        event.getJobPostingId(),
                                        event.getAnalysisType())
                        .orElseThrow(() -> new IllegalStateException("AI 분석 작업을 찾을 수 없습니다."));

        try {
            AiEvaluationAnalysisRequest request = AiEvaluationAnalysisRequest.builder()
                            .userId(String.valueOf(event.getUserId()))
                            .jobPostingId(String.valueOf(event.getJobPostingId()))
                            .build();
            switch (event.getAnalysisType()) {
                case EVALUATION -> {
                    AiEvaluationResultResponse result = aiServiceClient.analyzeEvaluation(request);
                    saveEvaluationResult(aiEvalJob.getJobApplication(), result);
                }
                case RESUME -> {
                    AiResumeAnalysisResponse result = aiServiceClient.analyzeResume(request);
                    saveResumeResult(aiEvalJob.getJobApplication(), result);
                }
                case PORTFOLIO -> {
                    AiPortfolioAnalysisResponse result = aiServiceClient.analyzePortfolio(request);
                    savePortfolioResult(aiEvalJob.getJobApplication(), result);
                }
                default -> throw new IllegalStateException("지원하지 않는 분석 타입: " + event.getAnalysisType());
            }

            aiEvalJob.complete();
            aiEvalJobRepository.save(aiEvalJob);
            log.info("AI 분석 성공: JobId={},Type={}", aiEvalJob.getId(), event.getAnalysisType());
        } catch (Exception e) {
            log.error("AI Worker Failed: JobID={}, Msg={}", aiEvalJob.getId(), event.getAnalysisType(), e.getMessage());
            aiEvalJob.fail(e.getMessage());
            aiEvalJobRepository.save(aiEvalJob);
        }
    }

    private void saveEvaluationResult(JobApplication application, AiEvaluationResultResponse result) {

        List<EvaluationCriteria> criteria = result.getEvaluationCriteria() != null
                        ? result.getEvaluationCriteria().stream()
                                        .map(c -> new EvaluationCriteria(c.getName(), c.getDescription()))
                                        .collect(Collectors.toList())
                        : null;

        List<EvaluationScore> evaluationScores = result.getCompetencyScores().stream()
                        .map(cs -> new EvaluationScore(cs.getName(), cs.getScore(), cs.getDescription()))
                        .collect(Collectors.toList());

        if (criteria != null) {
            JobMaster jobMaster = application.getJobMaster();
            jobMaster.updateEvaluationCriteria(criteria);
            jobMasterRepository.save(jobMaster);
        }

        Optional<AiApplicantEvaluation> existingOpt  = aiApplicationEvaluationRepository.findByJobApplicationId(application.getId());
        if (existingOpt.isPresent()) {
            AiApplicantEvaluation existing = existingOpt.get();
            existing.updateEvaluation(
                    result.getOverallScore(),
                    result.getOneLineReview(),
                    result.getFeedbackDetail(),
                    evaluationScores
            );
            aiApplicationEvaluationRepository.save(existing);
            log.info("AI 평가 결과 업데이트 완료: ApplicationId={}", application.getId());
        } else {
            AiApplicantEvaluation evaluation = AiApplicantEvaluation.builder()
                    .jobApplication(application)
                    .overallScore(result.getOverallScore())
                    .oneLineReview(result.getOneLineReview())
                    .feedbackDetail(result.getFeedbackDetail())
                    .comparisonScores(evaluationScores)
                    .build();

            aiApplicationEvaluationRepository.save(evaluation);
            log.info("AI 평가 결과 신규 저장 완료: {}", application.getId());
        }
    }

    private void saveResumeResult(JobApplication application, AiResumeAnalysisResponse result) {
        aiResumeAnalysisRepository.findByJobApplicationId(application.getId())
                .ifPresentOrElse(
                        existing -> {
                            existing.update(result.getAiAnalysisReport(), result.getJobFitScore(),
                                    result.getExperienceClarityScore(), result.getReadabilityScore());
                            aiResumeAnalysisRepository.save(existing);
                            log.info("이력서 분석 결과 업데이트 완료: ApplicationId={}", application.getId());
                        }, () -> {
                            aiResumeAnalysisRepository.save(AiResumeAnalysis.builder()
                                    .jobApplication(application)
                                    .aiAnalysisReport(result.getAiAnalysisReport())
                                    .jobFitScore(result.getJobFitScore())
                                    .experienceClarityScore(result.getExperienceClarityScore())
                                    .readabilityScore(result.getReadabilityScore())
                                    .build());
                            log.info("이력서 분석 결과 신규 저장 완료: ApplicationId={}", application.getId());
                        }
                );
    }

    private void savePortfolioResult(JobApplication application, AiPortfolioAnalysisResponse result) {
        aiPortfolioAnalysisRepository.findByJobApplicationId(application.getId())
                .ifPresentOrElse(
                        existing -> {
                            existing.update(result.getAiAnalysisReport(), result.getProblemSolvingScore(),
                                    result.getContributionClarityScore(), result.getTechnicalDepthScore());
                            aiPortfolioAnalysisRepository.save(existing);
                            log.info("포트폴리오 분석 결과 업데이트 완료: ApplicationId={}", application.getId());
                        },
                        () -> {
                            aiPortfolioAnalysisRepository.save(AiPortfolioAnalysis.builder()
                                    .jobApplication(application)
                                    .aiAnalysisReport(result.getAiAnalysisReport())
                                    .problemSolvingScore(result.getProblemSolvingScore())
                                    .contributionClarityScore(result.getContributionClarityScore())
                                    .technicalDepthScore(result.getTechnicalDepthScore())
                                    .build());
                            log.info("포트폴리오 분석 결과 신규 저장 완료: ApplicationId={}", application.getId());
                        }
                );
    }

}