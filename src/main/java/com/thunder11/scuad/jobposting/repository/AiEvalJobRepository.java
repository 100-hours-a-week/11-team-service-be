package com.thunder11.scuad.jobposting.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.thunder11.scuad.jobposting.domain.AiEvalJob;
import com.thunder11.scuad.jobposting.domain.type.AnalysisType;

public interface AiEvalJobRepository extends JpaRepository<AiEvalJob, Long> {

    Optional<AiEvalJob> findFirstByJobApplicationIdAndAnalysisTypeOrderByIdDesc(Long applicationId,
            AnalysisType analysisType);

    Optional<AiEvalJob> findFirstByRequestedByUserIdAndJobApplicationJobMasterIdOrderByIdDesc(Long userId,
            Long jobMasterId);
}
