package com.thunder11.scuad.infra.ai.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AiAnalysisTaskResponse {

    private Long applicationId;
    private Long evalJobId;
    private String status;
}
