package com.thunder11.scuad.jobposting.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;

@Getter
public class AiAnalysisRequest {
    @JsonProperty("analysis_type")
    private String analysisType;
}