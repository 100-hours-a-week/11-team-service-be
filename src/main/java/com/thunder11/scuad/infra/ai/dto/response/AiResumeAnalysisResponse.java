package com.thunder11.scuad.infra.ai.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.*;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class AiResumeAnalysisResponse {

    @JsonProperty("ai_analysis_report")
    private String aiAnalysisReport;

    @JsonProperty("job_fit_score")
    private String jobFitScore;

    @JsonProperty("experience_clarity_score")
    private String experienceClarityScore;

    @JsonProperty("readability_score")
    private String readabilityScore;
}