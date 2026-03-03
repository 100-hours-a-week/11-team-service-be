package com.thunder11.scuad.infra.ai.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AiCompareRequest {

    @JsonProperty("job_posting_id")
    private String jobPostingId;

    @JsonProperty("user_id")
    private String userId;

    @JsonProperty("competitor")
    private String competitor;
}