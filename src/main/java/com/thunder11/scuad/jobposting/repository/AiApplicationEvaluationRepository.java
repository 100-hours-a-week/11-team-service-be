package com.thunder11.scuad.jobposting.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.thunder11.scuad.jobposting.domain.AiApplicantEvaluation;

public interface AiApplicationEvaluationRepository extends JpaRepository<AiApplicantEvaluation, Long> {
    Optional<AiApplicantEvaluation> findByJobApplicationId(Long jobApplicationId);
}
