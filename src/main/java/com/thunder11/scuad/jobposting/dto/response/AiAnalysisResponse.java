package com.thunder11.scuad.jobposting.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AiAnalysisResponse {

    private Long applicationId;
    private Long evalJobId;
    private String status;
}
