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
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class AiEvaluationResultResponse {

    @JsonProperty("overall_score")
    private Integer overallScore;

    @JsonProperty("competency_scores")
    private List<CompetencyScore> competencyScores;

    @JsonProperty("one_line_review")
    private String oneLineReview;

    @JsonProperty("feedback_detail")
    private String feedbackDetail;

    @Getter
    @NoArgsConstructor
    @ToString
    public static class CompetencyScore {
        private String name;
        private Integer score;
        private String description;
    }
}