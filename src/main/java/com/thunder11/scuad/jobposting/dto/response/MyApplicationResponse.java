package com.thunder11.scuad.jobposting.dto.response;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.thunder11.scuad.jobposting.domain.JobApplication;

@Getter
@Builder
public class MyApplicationResponse {
    private Long id;
    private Long jobMasterId;
    private String companyName;
    private String jobTitle;
    private LocalDateTime appliedAt;

    @JsonProperty("overallScore")
    private Integer overallScore;

    @JsonProperty("isProcessing")
    private Boolean isProcessing;

    @JsonProperty("resumeAnalyzed")
    private Boolean resumeAnalyzed;

    @JsonProperty("portfolioAnalyzed")
    private Boolean portfolioAnalyzed;

    @JsonProperty("resumeRegistered")
    private Boolean resumeRegistered;

    @JsonProperty("portfolioRegistered")
    private Boolean portfolioRegistered;

    public static MyApplicationResponse from(JobApplication ja, Integer overallScore, Boolean isProcessing,
            Boolean resumeAnalyzed, Boolean portfolioAnalyzed, Boolean resumeRegistered, Boolean portfolioRegistered) {
        return MyApplicationResponse.builder()
                .id(ja.getId())
                .jobMasterId(ja.getJobMaster().getId())
                .companyName(ja.getJobMaster().getCompany().getName())
                .jobTitle(ja.getJobMaster().getJobTitle())
                .appliedAt(ja.getCreatedAt())
                .overallScore(overallScore)
                .isProcessing(isProcessing)
                .resumeAnalyzed(resumeAnalyzed)
                .portfolioAnalyzed(portfolioAnalyzed)
                .resumeRegistered(resumeRegistered)
                .portfolioRegistered(portfolioRegistered)
                .build();
    }
}