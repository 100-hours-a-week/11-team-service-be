package com.thunder11.scuad.infra.ai.dto.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class AiJobAnalysisResponse {

    @JsonProperty("job_posting_id")
    private Long jobPostingId;

    @JsonProperty("is_existing")
    private boolean isExisting;

    @JsonProperty("company_name")
    private String companyName;

    @JsonProperty("job_title")
    private String jobTitle;

    @JsonProperty("main_responsibilities")
    private List<String> mainResponsibilities;

    @JsonProperty("required_skills")
    private List<String> requiredSkills;

    @JsonProperty("recruitment_status")
    private String recruitmentStatus;

    @JsonProperty("recruitment_period")
    private RecruitmentPeriod recruitmentPeriod;

    @JsonProperty("ai_summary")
    private String aiSummary;

    @JsonProperty("evaluation_criteria")
    private List<EvaluationCriteria> evaluationCriteria;

    @Getter
    @NoArgsConstructor
    public static class RecruitmentPeriod {

        @JsonProperty("start_date")
        private String startDate;

        @JsonProperty("end_date")
        private String endDate;
    }

    @Getter
    @NoArgsConstructor
    public static class EvaluationCriteria {
        private String name;
        private String description;
    }
}