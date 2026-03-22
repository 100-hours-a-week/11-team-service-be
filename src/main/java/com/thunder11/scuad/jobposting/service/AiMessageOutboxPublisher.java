package com.thunder11.scuad.jobposting.service;

import java.util.List;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.thunder11.scuad.jobposting.domain.AiMessageOutbox;
import com.thunder11.scuad.jobposting.repository.AiMessageOutboxRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiMessageOutboxPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final AiMessageOutboxRepository outboxRepository;

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void publishPendingMessages() {
        List<AiMessageOutbox> pendings = outboxRepository.findAllByStatus(AiMessageOutbox.OutboxStatus.PENDING);
        
        for (AiMessageOutbox outbox : pendings) {
            CorrelationData correlationData = new CorrelationData(outbox.getId().toString());
            
            Message message = MessageBuilder.withBody(outbox.getPayload().getBytes())
                    .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                    .build();
            
            rabbitTemplate.send(outbox.getExchange(), outbox.getRoutingKey(), message, correlationData);
            
            outbox.sent();
            outboxRepository.save(outbox);
        }
    }
}
