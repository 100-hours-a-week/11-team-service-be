package com.thunder11.scuad.jobposting.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.thunder11.scuad.infra.ai.client.AiServiceClient;
import com.thunder11.scuad.infra.ai.dto.request.AiEvaluationAnalysisRequest;
import com.thunder11.scuad.jobposting.domain.AiEvalJob;
import com.thunder11.scuad.jobposting.domain.JobApplication;
import com.thunder11.scuad.jobposting.event.AiEvaluationCreateEvent;
import com.thunder11.scuad.jobposting.repository.AiEvalJobRepository;
import com.thunder11.scuad.jobposting.repository.JobApplicationRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiEvaluationWorker {

    private final AiEvalJobRepository aiEvalJobRepository;
    private final AiServiceClient aiServiceClient;
    private final JobApplicationRepository jobApplicationRepository;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void processEvaluationAsync(AiEvaluationCreateEvent event) {

        Long aiEvalJobId = event.getAiEvalJobId();

        log.info("AI 종합 분석 작업시작: JobId={}",  aiEvalJobId);

        AiEvalJob aiEvalJob = aiEvalJobRepository.findById(aiEvalJobId).orElseThrow();

        JobApplication jobApplication = jobApplicationRepository.findById(event.getJobApplicationId()).orElseThrow();

        try {
            AiEvaluationAnalysisRequest request = AiEvaluationAnalysisRequest.builder()
                    .userId(jobApplication.getUser().getUserId())
                    .jobPostingId(jobApplication.getJobMaster().getId())
                    .build();

            aiServiceClient.analyzeEvaluation(request);

            aiEvalJob.complete();
            log.info("AI 종합 분석 성공: JobId={}",  aiEvalJobId);
        } catch (Exception e) {
            log.error("AI Worker Failed: JobID={}, Msg={}", aiEvalJobId, e.getMessage());
            aiEvalJob.fail(e.getMessage());
        }
    }
}