package com.thunder11.scuad.jobposting.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.thunder11.scuad.jobposting.domain.AiPortfolioAnalysis;

public interface AiPortfolioAnalysisRepository extends JpaRepository<AiPortfolioAnalysis, Long> {
    Optional<AiPortfolioAnalysis> findByJobApplicationId(Long applicationId);
}
