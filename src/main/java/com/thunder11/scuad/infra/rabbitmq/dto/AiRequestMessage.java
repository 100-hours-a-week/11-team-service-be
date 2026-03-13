package com.thunder11.scuad.infra.rabbitmq.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiRequestMessage {
    @JsonProperty("eval_job_id")
    private String evalJobId;
    @JsonProperty("user_id")
    private String userId;
    @JsonProperty("job_posting_id")
    private String jobPostingId;
}
