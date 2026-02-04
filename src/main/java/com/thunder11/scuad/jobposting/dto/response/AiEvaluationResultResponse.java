package com.thunder11.scuad.jobposting.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

import com.thunder11.scuad.jobposting.domain.AiApplicantEvaluation;
import com.thunder11.scuad.jobposting.domain.EvaluationScore;

@Getter
@Builder
public class AiEvaluationResultResponse {

    private Long evaluationId;
    private Long jobApplicationId;
    private Integer overallScore;
    private String oneLineReview;
    private String feedbackDetail;
    private List<EvaluationScore> comparisonScores;
    private LocalDateTime analyzedAt;


    public static AiEvaluationResultResponse from(AiApplicantEvaluation entity) {
        return AiEvaluationResultResponse.builder()
                .evaluationId(entity.getId())
                .jobApplicationId(entity.getJobApplication().getId())
                .overallScore(entity.getOverallScore())
                .oneLineReview(entity.getOneLineReview())
                .feedbackDetail(entity.getFeedbackDetail())
                .comparisonScores(entity.getComparisonScores())
                .analyzedAt(entity.getCreatedAt())
                .build();
    }
}