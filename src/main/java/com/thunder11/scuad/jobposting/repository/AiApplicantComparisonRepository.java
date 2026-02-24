package com.thunder11.scuad.jobposting.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.thunder11.scuad.jobposting.domain.AiApplicantComparison;

public interface AiApplicantComparisonRepository extends JpaRepository<AiApplicantComparison, Long> {

    // (나, 상대) 조합으로 기존 비교 결과 조회 (중복 AI 호출 방지)
    Optional<AiApplicantComparison> findByMyApplication_IdAndCompetitorApplication_Id(
            Long myApplicationId,
            Long competitorApplicationId
    );
}