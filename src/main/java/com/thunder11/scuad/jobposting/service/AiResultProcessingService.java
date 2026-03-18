package com.thunder11.scuad.jobposting.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.thunder11.scuad.infra.ai.dto.response.AiCompareResponse;
import com.thunder11.scuad.infra.ai.dto.response.AiEvaluationResultResponse;
import com.thunder11.scuad.infra.ai.dto.response.AiPortfolioAnalysisResponse;
import com.thunder11.scuad.infra.ai.dto.response.AiResumeAnalysisResponse;
import com.thunder11.scuad.infra.rabbitmq.dto.AiResponseMessage;
import com.thunder11.scuad.jobposting.domain.*;
import com.thunder11.scuad.jobposting.repository.*;
import com.thunder11.scuad.notification.service.NotificationService;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiResultProcessingService {
    private final AiEvalJobRepository aiEvalJobRepository;
    private final AiApplicationEvaluationRepository aiApplicationEvaluationRepository;
    private final AiResumeAnalysisRepository aiResumeAnalysisRepository;
    private final AiPortfolioAnalysisRepository aiPortfolioAnalysisRepository;
    private final AiApplicantComparisonRepository aiApplicantComparisonRepository;
    private final JobMasterRepository jobMasterRepository;
    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;

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
                case COMPARISON -> {
                    // COMPARISON 타입은 my + competitor 두 지원이 모두 필요.
                    // AiEvalJob 생성 시 competitorApplication을 함께 저장해두었으므로
                    // 콜백 수신 시점에 별도 조회 없이 바로 저장 가능.
                    AiCompareResponse result = objectMapper.treeToValue(response.getData(), AiCompareResponse.class);
                    saveComparisonResult(aiEvalJob.getJobApplication(), aiEvalJob.getCompetitorApplication(), result);
                }
                default -> throw new IllegalArgumentException("지원하지 않는 분석 타입: " + aiEvalJob.getAnalysisType());
            }
            aiEvalJob.complete();
            aiEvalJobRepository.save(aiEvalJob);
            log.info("AI 분석 완료 처리 - evalJobId: {}, type: {}", evalJobId, aiEvalJob.getAnalysisType());
            Long userId = aiEvalJob.getJobApplication().getUser().getUserId();
            String company = aiEvalJob.getJobApplication().getJobMaster().getCompany().getName();
            String position = aiEvalJob.getJobApplication().getJobMaster().getJobTitle();
            String jobPostingTitleStr = company + "(" + position + ")";
            Long applicationId = aiEvalJob.getJobApplication().getId();
            String notifType = switch (aiEvalJob.getAnalysisType()) {
                case EVALUATION ->  "AI_EVAL_COMPLETE";
                case RESUME -> "RESUME_COMPLETE";
                case PORTFOLIO -> "PORTFOLIO_COMPLETE";
                case COMPARISON -> "COMPARISON_COMPLETE";
                default -> "ANALYSIS_COMPLETE";
            };

            try {
                notificationService.createAndPush(userId, notifType, jobPostingTitleStr, applicationId);
            } catch (Exception e) {
                log.error("알림 발송 중 오류 발생 (결과 저장에는 영향 없음)", e);
            }
        } catch (Exception e) {
            log.error("AI 결과 저장 실패 - evalJobId: {}", evalJobId, e);
            aiEvalJob.fail(e.getMessage());
            aiEvalJobRepository.save(aiEvalJob);
        }
    }

    private void saveComparisonResult(JobApplication myApplication,
                                       JobApplication competitorApplication,
                                       AiCompareResponse result) {
        List<ComparisonMetric> metrics = result.getComparisonMetrics().stream()
                .map(m -> new ComparisonMetric(m.getName(), m.getMyScore(), m.getCompetitorScore()))
                .collect(Collectors.toList());

        // UNIQUE 제약(my_application_id, competitor_application_id)으로
        // 동일 조합 중복 저장은 DB 레벨에서 방지됨
        aiApplicantComparisonRepository.findByMyApplication_IdAndCompetitorApplication_Id(
                myApplication.getId(), competitorApplication.getId()
        ).ifPresentOrElse(
                existing -> log.info("비교 결과 이미 존재, 저장 생략: myApplicationId={}, competitorApplicationId={}",
                        myApplication.getId(), competitorApplication.getId()),
                () -> {
                    AiApplicantComparison comparison = AiApplicantComparison.builder()
                            .jobMaster(myApplication.getJobMaster())
                            .myApplication(myApplication)
                            .competitorApplication(competitorApplication)
                            .comparisonMetrics(metrics)
                            .strengthsReport(result.getStrengthsReport())
                            .weaknessesReport(result.getWeaknessesReport())
                            .build();
                    aiApplicantComparisonRepository.save(comparison);
                    log.info("AI 비교 결과 저장 완료: myApplicationId={}, competitorApplicationId={}",
                            myApplication.getId(), competitorApplication.getId());
                }
        );
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
