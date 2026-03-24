package com.thunder11.scuad.jobposting.service;

import com.thunder11.scuad.jobposting.event.AiJobPostingAnalysisCreateEvent;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.thunder11.scuad.infra.rabbitmq.config.RabbitMQConfig;
import com.thunder11.scuad.infra.rabbitmq.dto.AiRequestMessage;
import com.thunder11.scuad.jobposting.event.AiAnalysisCreateEvent;
import com.thunder11.scuad.jobposting.event.AiComparisonCreateEvent;
import com.thunder11.scuad.jobposting.domain.*;
import com.thunder11.scuad.jobposting.repository.*;
import com.thunder11.scuad.jobposting.domain.type.AnalysisType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.event.EventListener;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiEvaluationWorker {

    private final AiEvalJobRepository aiEvalJobRepository;
    private final AiMessageOutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @EventListener
    @Transactional
    public void processJobPostingAnalysis(AiJobPostingAnalysisCreateEvent event) {
        AiEvalJob aiEvalJob = aiEvalJobRepository.findById(event.getEvalJobId())
                .orElseThrow(() -> new IllegalStateException("Job not found"));
        
        AiRequestMessage message = AiRequestMessage.builder()
                .evalJobId(String.valueOf(aiEvalJob.getId()))
                .userId(String.valueOf(event.getUserId()))
                .url(aiEvalJob.getSourceUrl())
                .build();
        
        aiEvalJob.startProcessing();
        aiEvalJobRepository.save(aiEvalJob);
        
        try {
            String payload = objectMapper.writeValueAsString(message);
            outboxRepository.save(new AiMessageOutbox(RabbitMQConfig.EXCHANGE_NAME, RabbitMQConfig.QUEUE_JOBPOSTING, payload));
        } catch (Exception e) {
            log.error("Outbox save failed", e);
        }
    }

    @EventListener
    @Transactional
    public void processEvaluation(AiAnalysisCreateEvent event) {
        AiEvalJob aiEvalJob = aiEvalJobRepository
                .findFirstByRequestedByUserIdAndJobApplicationJobMasterIdAndAnalysisTypeOrderByIdDesc(
                        event.getUserId(),
                        event.getJobPostingId(),
                        event.getAnalysisType())
                .orElseThrow(() -> new IllegalStateException("Job not found"));

        String routingKey = getQueueNameByType(event.getAnalysisType());

        AiRequestMessage message = AiRequestMessage.builder()
                .evalJobId(String.valueOf(aiEvalJob.getId()))
                .userId(String.valueOf(event.getUserId()))
                .jobPostingId(String.valueOf(event.getJobPostingId()))
                .build();

        aiEvalJob.startProcessing();
        aiEvalJobRepository.save(aiEvalJob);

        try {
            String payload = objectMapper.writeValueAsString(message);
            outboxRepository.save(new AiMessageOutbox(RabbitMQConfig.EXCHANGE_NAME, routingKey, payload));
        } catch (Exception e) {
            log.error("Outbox save failed", e);
        }
    }

    // COMPARISON 타입 전용 핸들러
    // 기존 AiAnalysisCreateEvent와 분리한 이유:
    //   COMPARISON은 competitor 정보가 추가로 필요하며,
    //   AiEvalJob이 이미 생성된 상태에서 이벤트를 발행하므로
    //   evalJobId로 직접 조회하는 방식을 사용한다.
    @EventListener
    @Transactional
    public void processComparison(AiComparisonCreateEvent event) {
        AiEvalJob aiEvalJob = aiEvalJobRepository.findById(event.getEvalJobId())
                .orElseThrow(() -> new IllegalStateException("Job not found"));

        AiRequestMessage message = AiRequestMessage.builder()
                .evalJobId(String.valueOf(aiEvalJob.getId()))
                .userId(String.valueOf(event.getUserId()))
                .jobPostingId(String.valueOf(event.getJobPostingId()))
                .competitor(String.valueOf(event.getCompetitorUserId()))
                .build();

        try {
            String payload = objectMapper.writeValueAsString(message);
            outboxRepository.save(new AiMessageOutbox(RabbitMQConfig.EXCHANGE_NAME, RabbitMQConfig.QUEUE_COMPARISON, payload));
        } catch (Exception e) {
            log.error("Outbox save failed", e);
        }
    }

    private String getQueueNameByType(AnalysisType type) {
        return switch (type) {
            case JOBPOSTING ->  RabbitMQConfig.QUEUE_JOBPOSTING;
            case EVALUATION -> RabbitMQConfig.QUEUE_EVALUATION;
            case RESUME -> RabbitMQConfig.QUEUE_RESUME;
            case PORTFOLIO -> RabbitMQConfig.QUEUE_PORTFOLIO;
            case COMPARISON ->  RabbitMQConfig.QUEUE_COMPARISON;

        };
    }
}
