package com.thunder11.scuad.jobposting.repository;

import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.thunder11.scuad.jobposting.domain.AiResumeAnalysis;

public interface AiResumeAnalysisRepository extends JpaRepository<AiResumeAnalysis, Long> {
    Optional<AiResumeAnalysis> findByJobApplicationId(long applicationId);
    List<AiResumeAnalysis> findAllByJobApplicationIdIn(List<Long> jobApplicationIds);
}
