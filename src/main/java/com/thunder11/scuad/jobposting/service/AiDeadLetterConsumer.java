package com.thunder11.scuad.jobposting.service;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.thunder11.scuad.infra.rabbitmq.config.RabbitMQConfig;
import com.thunder11.scuad.infra.rabbitmq.dto.AiRequestMessage;
import com.thunder11.scuad.jobposting.repository.AiEvalJobRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiDeadLetterConsumer {

    private final RabbitTemplate rabbitTemplate;
    private final AiEvalJobRepository aiEvalJobRepository;

    @Transactional
    @RabbitListener(queues = {
        "scuad.ai.queue.evaluation.dead_letter",
        "scuad.ai.queue.resume.dead_letter",
        "scuad.ai.queue.portfolio.dead_letter",
        "scuad.ai.queue.comparison.dead_letter",
        "scuad.ai.request.jobposting.queue.dead_letter"
    })
    public void handleDeadLetter(AiRequestMessage aiRequestMessage, Message rabbitMessage) {
        Long evalJobId = Long.parseLong(aiRequestMessage.getEvalJobId());
        Integer retryCount = (Integer) rabbitMessage.getMessageProperties().getHeaders().getOrDefault("x-retry-count", 0);

        if (retryCount < 3) {
            long delay = (long) Math.pow(2, retryCount) * 60000; 
            String originalRoutingKey = rabbitMessage.getMessageProperties().getReceivedRoutingKey().replace(".dead_letter", ".delay");

            log.info("Retry {} - JobId: {}", retryCount + 1, evalJobId);

            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, originalRoutingKey, aiRequestMessage, m -> {
                m.getMessageProperties().setExpiration(String.valueOf(delay));
                m.getMessageProperties().setHeader("x-retry-count", retryCount + 1);
                return m;
            });
        } else {
            aiEvalJobRepository.findById(evalJobId).ifPresent(job -> {
                log.error("Max retries reached - JobId: {}", evalJobId);
                job.fail("Max retries reached");
                aiEvalJobRepository.save(job);
            });
        }
    }
}
