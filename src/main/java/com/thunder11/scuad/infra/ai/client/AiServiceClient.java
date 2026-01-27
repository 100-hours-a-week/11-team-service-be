package com.thunder11.scuad.infra.ai.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.thunder11.scuad.common.exception.ApiException;
import com.thunder11.scuad.common.exception.ErrorCode;
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

    @Async
    public void deleteJobAnalysis(Long aiJobId) {
        log.info("AI 분석 데이터 비동기 삭제 요청 시작: ID={}", aiJobId);
        try {
            webClient.delete()
                    .uri(aiServiceUrl + "/api/v1/job-posting/" + aiJobId)
                    .retrieve()
                    .toBodilessEntity()
                    .block();
            log.info("AI 분석 데이터 삭제 완료: ID={}", aiJobId);
        } catch (Exception e) {
            log.error("AI 분석 데이터 삭제 실패: ID={}, 이유={}", aiJobId, e.getMessage());
        }
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