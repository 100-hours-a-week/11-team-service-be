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

        // ============================================================
        // [테스트 전용] 동기 블로킹 스레드 점유 재현 — 테스트 후 반드시 제거
        //
        // 의도: 실제 AI 서버가 없는 환경에서도 수 초 소요되는 AI 응답 대기 상황을
        //       정확히 재현하기 위해 3초 sleep 후 더미 응답을 반환.
        //       핵심은 AI 호출 성공 여부가 아니라 "스레드가 3초 동안 점유되는 현상"이므로
        //       더미 응답으로 대체해도 테스트 목적(스레드 풀 소진 측정)에 완전히 부합함.
        // ============================================================
        try { Thread.sleep(3000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        log.info("[테스트 전용] AI 더미 응답 반환 - userId={}", request.getUserId());
        return AiCompareResponse.dummy();
    }
}