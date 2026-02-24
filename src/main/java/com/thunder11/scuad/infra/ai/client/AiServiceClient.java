package com.thunder11.scuad.infra.ai.client;

import com.thunder11.scuad.infra.ai.dto.request.AiCompareRequest;
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
import com.thunder11.scuad.infra.ai.dto.request.AiEvaluationAnalysisRequest;
import com.thunder11.scuad.infra.ai.dto.response.*;

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
                .uri(aiServiceUrl + "/ai/api/v1/job-posting/analyze")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<AiApiResponse<AiJobAnalysisResponse>>() {
                })
                .block();

        validateAiResponse(response);

        return response.getData();
    }

    @Async
    public void deleteJobAnalysis(Long aiJobId) {
        log.info("AI 분석 데이터 비동기 삭제 요청 시작: ID={}", aiJobId);
        try {
            webClient.delete()
                    .uri(aiServiceUrl + "/ai/api/v1/job-posting/" + aiJobId)
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
            throw new ApiException(ErrorCode.AI_SERVICE_ERROR, "AI 분석 실패: " + msg);
        }
    }

    public AiEvaluationResultResponse analyzeEvaluation(AiEvaluationAnalysisRequest request) {
        log.info("AI 분석 요청: User={}, Job={}", request.getUserId(), request.getJobPostingId());

        try {
            AiApiResponse<AiEvaluationResultResponse> response = webClient.post()
                    .uri(aiServiceUrl + "/ai/api/v1/applicant/evaluate")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<AiApiResponse<AiEvaluationResultResponse>>() {
                    })
                    .block();

            log.info("AI 분석 결과: {}", response.getData());
            return response.getData();
        } catch (Exception e) {
            throw new ApiException(ErrorCode.AI_SERVICE_ERROR, "AI 호출 실패: " + e.getMessage());
        }
    }

    public AiResumeAnalysisResponse analyzeResume(AiEvaluationAnalysisRequest request) {
        log.info("AI 이력서 분석 요청: User={}, Job={}",
                request.getUserId(), request.getJobPostingId());
        try {
            AiApiResponse<AiResumeAnalysisResponse> response =
                    webClient.post()
                            .uri(aiServiceUrl + "/ai/api/v1/resume/analyze")
                            .bodyValue(request)
                            .retrieve()
                            .bodyToMono(new ParameterizedTypeReference<AiApiResponse<AiResumeAnalysisResponse>>() {})
                            .block();
            validateAiResponse(response);

            return response.getData();
        } catch (Exception e) {
            throw new ApiException(ErrorCode.AI_SERVICE_ERROR, "AI 이력서 분석 호출 실패: " + e.getMessage());
        }
    }

    public AiPortfolioAnalysisResponse analyzePortfolio(AiEvaluationAnalysisRequest request) {
        log.info("AI 포트폴리오 분석 요청: User={}, Job={}", request.getUserId(), request.getJobPostingId());
        try {
            AiApiResponse<AiPortfolioAnalysisResponse> response = webClient.post()
                    .uri(aiServiceUrl + "/ai/api/v1/portfolio/analyze")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<AiApiResponse<AiPortfolioAnalysisResponse>>() {})
                    .block();
            validateAiResponse(response);

            return response.getData();
        } catch (Exception e) {
            throw new ApiException(ErrorCode.AI_SERVICE_ERROR, "AI 포트폴리오 분석 호출 실패: " + e.getMessage());
        }
    }

    public AiCompareResponse compareApplicants(AiCompareRequest request) {
        log.info("AI 지원자 비교 요청: userId={}, competitor={}, jobPostingId={}",
                request.getUserId(), request.getCompetitor(), request.getJobPostingId());

        try {
            AiApiResponse<AiCompareResponse> response = webClient.post()
                    .uri(aiServiceUrl + "/ai/api/v1/applicant/compare")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<AiApiResponse<AiCompareResponse>>() {})
                    .block();

            validateAiResponse(response);
            return response.getData();
        } catch (Exception e) {
            throw new ApiException(ErrorCode.AI_SERVICE_ERROR, "AI 비교 분석 호출 실패: " + e.getMessage());
        }
    }
}