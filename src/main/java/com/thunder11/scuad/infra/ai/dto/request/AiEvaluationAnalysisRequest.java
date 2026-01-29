package com.thunder11.scuad.infra.ai.dto.request;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AiEvaluationAnalysisRequest {

    private Long userId;
    private Long jobPostingId;
}
