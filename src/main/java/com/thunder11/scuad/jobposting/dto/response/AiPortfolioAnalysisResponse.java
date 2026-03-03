package com.thunder11.scuad.jobposting.dto.response;

import lombok.Builder;
import lombok.Getter;

import com.thunder11.scuad.jobposting.domain.AiPortfolioAnalysis;

@Getter
@Builder
public class AiPortfolioAnalysisResponse {
    private Long analysisId;
    private String aiAnalysisReport;
    private String problemSolvingScore;
    private String contributionClarityScore;
    private String technicalDepthScore;

    public static AiPortfolioAnalysisResponse from(AiPortfolioAnalysis entity) {
        return AiPortfolioAnalysisResponse.builder()
                .analysisId(entity.getId())
                .aiAnalysisReport(entity.getAiAnalysisReport())
                .problemSolvingScore(entity.getProblemSolvingScore())
                .contributionClarityScore(entity.getContributionClarityScore())
                .technicalDepthScore(entity.getTechnicalDepthScore())
                .build();
    }
}
