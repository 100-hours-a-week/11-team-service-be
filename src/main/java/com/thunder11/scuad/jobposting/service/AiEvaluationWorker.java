package com.thunder11.scuad.jobposting.service;

import java.util.List;
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
import com.thunder11.scuad.jobposting.domain.AiApplicantEvaluation;
import com.thunder11.scuad.jobposting.domain.AiEvalJob;
import com.thunder11.scuad.jobposting.domain.EvaluationScore;
import com.thunder11.scuad.jobposting.domain.JobApplication;
import com.thunder11.scuad.jobposting.event.AiEvaluationCreateEvent;
import com.thunder11.scuad.jobposting.repository.AiApplicationEvaluationRepository;
import com.thunder11.scuad.jobposting.repository.AiEvalJobRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiEvaluationWorker {

    private final AiEvalJobRepository aiEvalJobRepository;
    private final AiApplicationEvaluationRepository aiApplicationEvaluationRepository;
    private final AiServiceClient aiServiceClient;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void processEvaluationAsync(AiEvaluationCreateEvent event) {

        log.info("AI 종합 분석 작업시작: UserId={}, JobPostingId={}",
                event.getUserId(), event.getJobPostingId());

        AiEvalJob aiEvalJob = aiEvalJobRepository
                .findFirstByRequestedByUserIdAndJobApplicationJobMasterIdOrderByIdDesc(
                        event.getUserId(),
                        event.getJobPostingId())
                .orElseThrow(() -> new IllegalStateException("AI 평가 작업을 찾을 수 없습니다."));

        Long aiEvalJobId = aiEvalJob.getId();

        try {
            AiEvaluationAnalysisRequest request = AiEvaluationAnalysisRequest.builder()
                    .userId(String.valueOf(event.getUserId()))
                    .jobPostingId(String.valueOf(event.getJobPostingId()))
                    .build();

            AiEvaluationResultResponse result = aiServiceClient.analyzeEvaluation(request);

            saveEvaluationResult(aiEvalJob.getJobApplication(), result);

            aiEvalJob.complete();
            aiEvalJobRepository.save(aiEvalJob);
            log.info("AI 종합 분석 성공: JobId={}", aiEvalJobId);
        } catch (Exception e) {
            log.error("AI Worker Failed: JobID={}, Msg={}", aiEvalJobId, e.getMessage());
            aiEvalJob.fail(e.getMessage());
            aiEvalJobRepository.save(aiEvalJob);
        }
    }

    private void saveEvaluationResult(JobApplication application, AiEvaluationResultResponse result) {

        List<EvaluationScore> evaluationScores = result.getCompetencyScores().stream()
                .map(cs -> new EvaluationScore(cs.getName(), cs.getScore(), cs.getDescription()))
                .collect(Collectors.toList());

        AiApplicantEvaluation evaluation = AiApplicantEvaluation.builder()
                .jobApplication(application)
                .overallScore(result.getOverallScore())
                .oneLineReview(result.getOneLineReview())
                .feedbackDetail(result.getFeedbackDetail())
                .comparisonScores(evaluationScores)
                .build();

        aiApplicationEvaluationRepository.save(evaluation);
    }
}