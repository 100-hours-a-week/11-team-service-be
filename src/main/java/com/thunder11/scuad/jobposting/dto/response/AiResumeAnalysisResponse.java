package com.thunder11.scuad.jobposting.dto.response;

import lombok.Builder;
import lombok.Getter;

import com.thunder11.scuad.jobposting.domain.AiResumeAnalysis;

@Getter
@Builder
public class AiResumeAnalysisResponse {
    private Long analysisId;
    private String aiAnalysisReport;
    private String jobFitScore;
    private String experienceClarityScore;
    private String readabilityScore;
    private Boolean isProcessing;

    public static AiResumeAnalysisResponse from(AiResumeAnalysis entity) {
        return AiResumeAnalysisResponse.builder()
                .analysisId(entity.getId())
                .aiAnalysisReport(entity.getAiAnalysisReport())
                .jobFitScore(entity.getJobFitScore())
                .experienceClarityScore(entity.getExperienceClarityScore())
                .readabilityScore(entity.getReadabilityScore())
                .build();
    }
}