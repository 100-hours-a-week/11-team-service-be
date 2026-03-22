package com.thunder11.scuad.jobposting.repository;

import com.thunder11.scuad.jobposting.domain.AiMessageOutbox;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AiMessageOutboxRepository extends JpaRepository<AiMessageOutbox, Long> {
    List<AiMessageOutbox> findAllByStatus(AiMessageOutbox.OutboxStatus status);
}
