package com.thunder11.scuad.jobposting.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.thunder11.scuad.jobposting.domain.AiEvalJob;
import com.thunder11.scuad.jobposting.domain.type.AnalysisType;
import com.thunder11.scuad.jobposting.domain.type.AiJobStatus;

public interface AiEvalJobRepository extends JpaRepository<AiEvalJob, Long> {

        Optional<AiEvalJob> findFirstByJobApplicationIdAndStatusInOrderByIdDesc(Long applicationId, List<AiJobStatus> statuses);

        Optional<AiEvalJob> findFirstByJobApplicationIdAndAnalysisTypeOrderByIdDesc(Long applicationId,
                        AnalysisType analysisType);

        Optional<AiEvalJob> findFirstByRequestedByUserIdAndJobApplicationJobMasterIdAndAnalysisTypeOrderByIdDesc(
                        Long userId,
                        Long jobMasterId, AnalysisType analysisType);

        Optional<AiEvalJob> findFirstBySourceUrlAndStatusInOrderByIdDesc(String sourceUrl,
                        java.util.Collection<com.thunder11.scuad.jobposting.domain.type.AiJobStatus> statuses);
}
