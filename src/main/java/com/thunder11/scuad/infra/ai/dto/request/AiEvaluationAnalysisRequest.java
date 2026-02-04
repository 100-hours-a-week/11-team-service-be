package com.thunder11.scuad.infra.ai.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AiEvaluationAnalysisRequest {

    @JsonProperty("user_id")
    private String userId;

    @JsonProperty("job_posting_id")
    private String jobPostingId;
}
