package com.thunder11.scuad.infra.ai.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.*;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class AiPortfolioAnalysisResponse {

    @JsonProperty("ai_analysis_report")
    private String aiAnalysisReport;

    @JsonProperty("problem_solving_score")
    private String problemSolvingScore;

    @JsonProperty("contribution_clarity_score")
    private String contributionClarityScore;

    @JsonProperty("technical_depth_score")
    private String technicalDepthScore;

}
