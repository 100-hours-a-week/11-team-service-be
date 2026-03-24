package com.thunder11.scuad.jobposting.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.thunder11.scuad.infra.ai.dto.response.*;
import com.thunder11.scuad.jobposting.domain.type.AnalysisType;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.thunder11.scuad.infra.rabbitmq.dto.AiResponseMessage;
import com.thunder11.scuad.jobposting.domain.*;
import com.thunder11.scuad.jobposting.repository.*;
import com.thunder11.scuad.notification.event.AiAnalysisCompleteEvent;

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
    private final ApplicationEventPublisher eventPublisher;
    private final JobPostingAnalysisService jobPostingAnalysisService;

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
            Long jobMasterId = null;
            switch (aiEvalJob.getAnalysisType()) {
                case JOBPOSTING -> {
                    AiJobAnalysisResponse result = objectMapper.treeToValue(response.getData(), AiJobAnalysisResponse.class);
                    jobMasterId = jobPostingAnalysisService.saveAnalysisResult(aiEvalJob.getId(), result);
                }
                case EVALUATION -> {
                    AiEvaluationResultResponse result = objectMapper.treeToValue(response.getData(),
                            AiEvaluationResultResponse.class);
                    saveEvaluationResult(aiEvalJob.getJobApplication(), result);
                }
                case RESUME -> {
                    AiResumeAnalysisResponse result = objectMapper.treeToValue(response.getData(),
                            AiResumeAnalysisResponse.class);
                    saveResumeResult(aiEvalJob.getJobApplication(), result);
                }
                case PORTFOLIO -> {
                    AiPortfolioAnalysisResponse result = objectMapper.treeToValue(response.getData(),
                            AiPortfolioAnalysisResponse.class);
                    savePortfolioResult(aiEvalJob.getJobApplication(), result);
                }
                case COMPARISON -> {
                    AiCompareResponse result = objectMapper.treeToValue(response.getData(), AiCompareResponse.class);
                    saveComparisonResult(aiEvalJob.getJobApplication(), aiEvalJob.getCompetitorApplication(), result);
                }
                default -> throw new IllegalArgumentException("지원하지 않는 분석 타입: " + aiEvalJob.getAnalysisType());
            }
            aiEvalJob.complete();
            aiEvalJobRepository.save(aiEvalJob);
            log.info("AI 분석 완료 처리 - evalJobId: {}, type: {}", evalJobId, aiEvalJob.getAnalysisType());

            // JOBPOSTING은 jobApplication이 null이므로 requestedBy에서 userId를 가져와야 함 (NPE 방지)
            Long userId = aiEvalJob.getRequestedBy().getUserId();

            if (aiEvalJob.getAnalysisType() == AnalysisType.JOBPOSTING) {
                eventPublisher.publishEvent(new AiAnalysisCompleteEvent(userId, "JOB_POSTING_COMPLETE", "채용공고 분석이 완료되었습니다.", jobMasterId));
            } else {
                JobApplication app = aiEvalJob.getJobApplication();
                String company = app.getJobMaster().getCompany().getName();
                String position = app.getJobMaster().getJobTitle();
                String jobPostingTitleStr = company + "(" + position + ")";

                String notifType = switch (aiEvalJob.getAnalysisType()) {
                    case EVALUATION -> "AI_EVAL_COMPLETE";
                    case RESUME -> "RESUME_COMPLETE";
                    case PORTFOLIO -> "PORTFOLIO_COMPLETE";
                    case COMPARISON -> "COMPARISON_COMPLETE";
                    default -> "ANALYSIS_COMPLETE";
                };
                eventPublisher.publishEvent(new AiAnalysisCompleteEvent(userId, notifType, jobPostingTitleStr, app.getId()));
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

        aiApplicantComparisonRepository.findByMyApplication_IdAndCompetitorApplication_Id(
                myApplication.getId(), competitorApplication.getId()).ifPresentOrElse(
                        existing -> log.info("비교 결과 이미 존재, 저장 생략: myApplicationId={}, competitorApplicationId={}",
                                myApplication.getId(), competitorApplication.getId()),
                        () -> {
                            AiApplicantComparison comparison = AiApplicantComparison.builder()
                                    .jobMaster(myApplication.getJobMaster())
                                    .myApplication(myApplication)
                                    .competitorApplication(competitorApplication)
                                    .comparisonMetrics(metrics)
                                    .strengthsReport(formatToMarkdown(result.getStrengthsReport()))
                                    .weaknessesReport(formatToMarkdown(result.getWeaknessesReport()))
                                    .build();
                            aiApplicantComparisonRepository.save(comparison);
                            log.info("AI 비교 결과 저장 완료: myApplicationId={}, competitorApplicationId={}",
                                    myApplication.getId(), competitorApplication.getId());
                        });
    }

    private void saveEvaluationResult(JobApplication application, AiEvaluationResultResponse result) {
        List<EvaluationCriteria> criteria = result.getEvaluationCriteria() != null
                ? result.getEvaluationCriteria().stream()
                        .map(c -> new EvaluationCriteria(c.getName(), c.getDescription()))
                        .collect(Collectors.toList())
                : null;

        List<EvaluationScore> evaluationScores = result.getCompetencyScores().stream()
                .map(cs -> new EvaluationScore(cs.getName(), cs.getScore(), formatToMarkdown(cs.getDescription())))
                .collect(Collectors.toList());

        if (criteria != null) {
            JobMaster jobMaster = application.getJobMaster();
            jobMaster.updateEvaluationCriteria(criteria);
            jobMasterRepository.save(jobMaster);
        }

        String formattedFeedback = formatToMarkdown(result.getFeedbackDetail());

        Optional<AiApplicantEvaluation> existingOpt = aiApplicationEvaluationRepository
                .findByJobApplicationId(application.getId());
        if (existingOpt.isPresent()) {
            AiApplicantEvaluation existing = existingOpt.get();
            existing.updateEvaluation(
                    result.getOverallScore(),
                    result.getOneLineReview(),
                    formattedFeedback,
                    evaluationScores);
            aiApplicationEvaluationRepository.save(existing);
            log.info("AI 평가 결과 업데이트 완료 (가공 적용): ApplicationId={}", application.getId());
        } else {
            AiApplicantEvaluation evaluation = AiApplicantEvaluation.builder()
                    .jobApplication(application)
                    .overallScore(result.getOverallScore())
                    .oneLineReview(result.getOneLineReview())
                    .feedbackDetail(formattedFeedback)
                    .comparisonScores(evaluationScores)
                    .build();

            aiApplicationEvaluationRepository.save(evaluation);
            log.info("AI 평가 결과 신규 저장 완료 (가공 적용): {}", application.getId());
        }
    }

    private void saveResumeResult(JobApplication application, AiResumeAnalysisResponse result) {
        String formattedReport = formatToMarkdown(result.getAiAnalysisReport());
        aiResumeAnalysisRepository.findByJobApplicationId(application.getId())
                .ifPresentOrElse(
                        existing -> {
                            existing.update(formattedReport,
                                    formatToMarkdown(result.getJobFitScore()),
                                    formatToMarkdown(result.getExperienceClarityScore()),
                                    formatToMarkdown(result.getReadabilityScore()));
                            aiResumeAnalysisRepository.save(existing);
                            log.info("이력서 분석 결과 업데이트 완료 (가공 적용): ApplicationId={}", application.getId());
                        }, () -> {
                            aiResumeAnalysisRepository.save(AiResumeAnalysis.builder()
                                    .jobApplication(application)
                                    .aiAnalysisReport(formattedReport)
                                    .jobFitScore(formatToMarkdown(result.getJobFitScore()))
                                    .experienceClarityScore(formatToMarkdown(result.getExperienceClarityScore()))
                                    .readabilityScore(formatToMarkdown(result.getReadabilityScore()))
                                    .build());
                            log.info("이력서 분석 결과 신규 저장 완료 (가공 적용): ApplicationId={}", application.getId());
                        });
    }

    private void savePortfolioResult(JobApplication application, AiPortfolioAnalysisResponse result) {
        String formattedReport = formatToMarkdown(result.getAiAnalysisReport());
        aiPortfolioAnalysisRepository.findByJobApplicationId(application.getId())
                .ifPresentOrElse(
                        existing -> {
                            existing.update(formattedReport,
                                    formatToMarkdown(result.getProblemSolvingScore()),
                                    formatToMarkdown(result.getContributionClarityScore()),
                                    formatToMarkdown(result.getTechnicalDepthScore()));
                            aiPortfolioAnalysisRepository.save(existing);
                            log.info("포트폴리오 분석 결과 업데이트 완료 (가공 적용): ApplicationId={}", application.getId());
                        },
                        () -> {
                            aiPortfolioAnalysisRepository.save(AiPortfolioAnalysis.builder()
                                    .jobApplication(application)
                                    .aiAnalysisReport(formattedReport)
                                    .problemSolvingScore(formatToMarkdown(result.getProblemSolvingScore()))
                                    .contributionClarityScore(formatToMarkdown(result.getContributionClarityScore()))
                                    .technicalDepthScore(formatToMarkdown(result.getTechnicalDepthScore()))
                                    .build());
                            log.info("포트폴리오 분석 결과 신규 저장 완료 (가공 적용): ApplicationId={}", application.getId());
                        });
    }

    /**
     * AI가 생성한 줄글 텍스트를 마크다운 형식으로 가공하여 가독성을 높입니다.
     */
    private String formatToMarkdown(String text) {
        if (text == null || text.isBlank()) return text;

        // 문장 단위 줄바꿈을 모든 글자(한글, 영문 등)에 대해 적용합니다.
        return text.replaceAll("\\. +", ".\n\n").trim();
    }
}
