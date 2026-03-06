package com.thunder11.scuad.jobposting.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.thunder11.scuad.infra.ai.dto.response.AiEvaluationResultResponse;
import com.thunder11.scuad.infra.ai.dto.response.AiPortfolioAnalysisResponse;
import com.thunder11.scuad.infra.ai.dto.response.AiResumeAnalysisResponse;
import com.thunder11.scuad.infra.rabbitmq.dto.AiResponseMessage;
import com.thunder11.scuad.jobposting.domain.*;
import com.thunder11.scuad.jobposting.repository.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiResultProcessingService {
    private final AiEvalJobRepository aiEvalJobRepository;
    private final AiApplicationEvaluationRepository aiApplicationEvaluationRepository;
    private final AiResumeAnalysisRepository aiResumeAnalysisRepository;
    private final AiPortfolioAnalysisRepository aiPortfolioAnalysisRepository;
    private final JobMasterRepository jobMasterRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void processResult(String evalJobId, AiResponseMessage response) {
        AiEvalJob aiEvalJob = aiEvalJobRepository.findById(Long.parseLong(evalJobId))
                .orElseThrow(() -> new IllegalArgumentException("AI 분석 작업을 찾을 수 없습니다. ID=" + evalJobId));

        if (!response.isSuccess()) {
            String errorMsg = (response.getError() != null) ? response.getError().getMessage() : "알 수 없는 오류";
            log.error("AI 분석 실패 - evalJobId: {}, 사유: {}", evalJobId, errorMsg);
            aiEvalJob.fail(errorMsg);
            aiEvalJobRepository.save(aiEvalJob);
            return;
        }
        try {
            switch (aiEvalJob.getAnalysisType()) {
                case EVALUATION -> {
                    AiEvaluationResultResponse result = objectMapper.treeToValue(response.getData(), AiEvaluationResultResponse.class);
                    saveEvaluationResult(aiEvalJob.getJobApplication(), result);
                }
                case RESUME -> {
                    AiResumeAnalysisResponse result = objectMapper.treeToValue(response.getData(), AiResumeAnalysisResponse.class);
                    saveResumeResult(aiEvalJob.getJobApplication(), result);
                }
                case PORTFOLIO -> {
                    AiPortfolioAnalysisResponse result = objectMapper.treeToValue(response.getData(), AiPortfolioAnalysisResponse.class);
                    savePortfolioResult(aiEvalJob.getJobApplication(), result);
                }
                default -> throw new IllegalArgumentException("지원하지 않는 분석 타입: " + aiEvalJob.getAnalysisType());
            }
            aiEvalJob.complete();
            aiEvalJobRepository.save(aiEvalJob);
            log.info("AI 분석 완료 처리 - evalJobId: {}, type: {}", evalJobId, aiEvalJob.getAnalysisType());
        } catch (Exception e) {
            log.error("AI 결과 저장 실패 - evalJobId: {}", evalJobId, e);
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

        Optional<AiApplicantEvaluation> existingOpt = aiApplicationEvaluationRepository
                .findByJobApplicationId(application.getId());
        if (existingOpt.isPresent()) {
            AiApplicantEvaluation existing = existingOpt.get();
            existing.updateEvaluation(
                    result.getOverallScore(),
                    result.getOneLineReview(),
                    result.getFeedbackDetail(),
                    evaluationScores);
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
                        });
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
                        });
    }
}
