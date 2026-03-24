package com.thunder11.scuad.jobposting.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.thunder11.scuad.jobposting.domain.AiEvalJob;
import com.thunder11.scuad.jobposting.domain.type.AiJobStatus;
import com.thunder11.scuad.jobposting.repository.AiEvalJobRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiJobMonitoringScheduler {

    private final AiEvalJobRepository aiEvalJobRepository;

    @Scheduled(fixedDelay = 600000)
    @Transactional
    public void recoverStuckJobs() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(20);
        List<AiEvalJob> stuckJobs = aiEvalJobRepository.findAllByStatusAndUpdatedAtBefore(
            AiJobStatus.PROCESSING, threshold
        );

        if (!stuckJobs.isEmpty()) {
            log.warn("Stuck jobs found: {}", stuckJobs.size());
            for (AiEvalJob job : stuckJobs) {
                log.info("Recovering jobId: {}", job.getId());
                job.fail("Stale job recovery");
                aiEvalJobRepository.save(job);
            }
        }
    }
}
