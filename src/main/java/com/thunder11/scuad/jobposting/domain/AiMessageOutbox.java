package com.thunder11.scuad.jobposting.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "ai_message_outbox")
public class AiMessageOutbox {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String exchange;
    private String routingKey;
    
    @Column(columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    private OutboxStatus status = OutboxStatus.PENDING;

    private int retryCount = 0;
    private LocalDateTime createdAt = LocalDateTime.now();

    public AiMessageOutbox(String exchange, String routingKey, String payload) {
        this.exchange = exchange;
        this.routingKey = routingKey;
        this.payload = payload;
    }

    public void sent() {
        this.status = OutboxStatus.SENT;
    }

    public enum OutboxStatus {
        PENDING, SENT, FAILED
    }
}
