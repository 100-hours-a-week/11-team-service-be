package com.thunder11.scuad.jobposting.service;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.thunder11.scuad.infra.rabbitmq.config.RabbitMQConfig;
import com.thunder11.scuad.infra.rabbitmq.dto.AiRequestMessage;
import com.thunder11.scuad.jobposting.event.AiAnalysisCreateEvent;
import com.thunder11.scuad.jobposting.domain.*;
import com.thunder11.scuad.jobposting.repository.*;
import com.thunder11.scuad.jobposting.domain.type.AnalysisType;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiEvaluationWorker {

    private final AiEvalJobRepository aiEvalJobRepository;
    private final RabbitTemplate rabbitTemplate;

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

        String routingKey = getQueueNameByType(event.getAnalysisType());

        AiRequestMessage message = AiRequestMessage.builder()
                .evalJobId(String.valueOf(aiEvalJob.getId()))
                .userId(String.valueOf(event.getUserId()))
                .jobPostingId(String.valueOf(event.getJobPostingId()))
                .build();

        aiEvalJob.startProcessing();
        aiEvalJobRepository.save(aiEvalJob);

        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, routingKey, message);
        log.info("RabbitMQ 발송 완료 - Queue: {}, evalJobId: {}", routingKey, aiEvalJob.getId());
    }

    private String getQueueNameByType(AnalysisType type) {
        return switch (type) {
            case EVALUATION -> RabbitMQConfig.QUEUE_EVALUATION;
            case RESUME -> RabbitMQConfig.QUEUE_RESUME;
            case PORTFOLIO -> RabbitMQConfig.QUEUE_PORTFOLIO;
            case COMPARISON ->  RabbitMQConfig.QUEUE_COMPARISON;
        };
    }
}
