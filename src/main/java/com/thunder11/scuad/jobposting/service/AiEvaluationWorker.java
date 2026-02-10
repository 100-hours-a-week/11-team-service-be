package com.thunder11.scuad.jobposting.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.thunder11.scuad.infra.ai.client.AiServiceClient;
import com.thunder11.scuad.infra.ai.dto.request.AiEvaluationAnalysisRequest;
import com.thunder11.scuad.infra.ai.dto.response.AiEvaluationResultResponse;
import com.thunder11.scuad.jobposting.domain.AiApplicantEvaluation;
import com.thunder11.scuad.jobposting.domain.AiEvalJob;
import com.thunder11.scuad.jobposting.domain.EvaluationCriteria;
import com.thunder11.scuad.jobposting.domain.EvaluationScore;
import com.thunder11.scuad.jobposting.domain.JobApplication;
import com.thunder11.scuad.jobposting.event.AiEvaluationCreateEvent;
import com.thunder11.scuad.jobposting.repository.AiApplicationEvaluationRepository;
import com.thunder11.scuad.jobposting.repository.AiEvalJobRepository;
import com.thunder11.scuad.jobposting.repository.JobMasterRepository;
import com.thunder11.scuad.jobposting.domain.JobMaster;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiEvaluationWorker {

        private final AiEvalJobRepository aiEvalJobRepository;
        private final AiApplicationEvaluationRepository aiApplicationEvaluationRepository;
        private final JobMasterRepository jobMasterRepository;
        private final AiServiceClient aiServiceClient;

        @Async
        @Transactional(propagation = Propagation.REQUIRES_NEW)
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

                // Idempotency: 이미 평가 결과가 저장되어 있으면 중복 insert를 피한다.
                // (부하테스트/재시도/중복 이벤트 발행 시 uk_applicant_evaluation_application 충돌 방지)
                if (aiApplicationEvaluationRepository.findByJobApplicationId(application.getId()).isPresent()) {
                        log.info("이미 AI 평가 결과 존재: applicationId={}", application.getId());
                        return;
                }

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

                AiApplicantEvaluation evaluation = AiApplicantEvaluation.builder()
                                .jobApplication(application)
                                .overallScore(result.getOverallScore())
                                .oneLineReview(result.getOneLineReview())
                                .feedbackDetail(result.getFeedbackDetail())
                                .comparisonScores(evaluationScores)
                                .build();

                try {
                        aiApplicationEvaluationRepository.save(evaluation);
                } catch (org.springframework.dao.DataIntegrityViolationException e) {
                        // DB 유니크 제약으로 중복 저장이 막힌 경우: 이미 다른 워커가 저장한 것으로 보고 무시한다.
                        log.warn("평가 결과 중복 저장 감지(무시): applicationId={}, msg={}", application.getId(), e.getMessage());
                }
        }
}