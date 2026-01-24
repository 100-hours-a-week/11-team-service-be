package com.thunder11.scuad.infra.ai.client;

import com.thunder11.scuad.common.exception.ApiException;
import com.thunder11.scuad.common.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.thunder11.scuad.infra.ai.dto.request.AiJobAnalysisRequest;
import com.thunder11.scuad.infra.ai.dto.response.AiApiResponse;
import com.thunder11.scuad.infra.ai.dto.response.AiJobAnalysisResponse;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiServiceClient {

    private final WebClient webClient;

    @Value("${ai.service.url}")
    private String aiServiceUrl;

    public AiJobAnalysisResponse analyzeJob(AiJobAnalysisRequest request) {
        log.info("AI 분석 요청 시작: {}", request.getUrl());

        AiApiResponse<AiJobAnalysisResponse> response = webClient.post()
                .uri(aiServiceUrl + "/api/v1/job-posting/analyze")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<AiApiResponse<AiJobAnalysisResponse>>() {})
                .block();

        validateAiResponse(response);

        return response.getData();
    }

    private void validateAiResponse(AiApiResponse<?> response) {
        if (response == null || !response.isSuccess() || response.getData() == null) {
            String msg = (response != null && response.getError() != null)
                    ? response.getError().getMessage()
                    : "Unknown AI Error";
            throw new ApiException(ErrorCode.AI_SERVICE_ERROR, "AI 분석 실패: "+ msg);
        }
    }
}