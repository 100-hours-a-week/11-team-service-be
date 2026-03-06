package com.thunder11.scuad.infra.rabbitmq.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.Getter;
import lombok.NoArgsConstructor;

import com.thunder11.scuad.infra.ai.dto.response.AiApiResponse;

@Getter
@NoArgsConstructor
public class AiResponseMessage {
    @JsonProperty("eval_job_id")
    private String evalJobId;

    private boolean success;
    private String timestamp;
    private JsonNode data;
    private AiApiResponse.AiError error;
}
