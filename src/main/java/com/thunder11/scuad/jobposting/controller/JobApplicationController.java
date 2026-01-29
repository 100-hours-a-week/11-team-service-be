package com.thunder11.scuad.jobposting.controller;

import jakarta.validation.Valid;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;

import com.thunder11.scuad.auth.security.UserPrincipal;
import com.thunder11.scuad.common.response.ApiResponse;
import com.thunder11.scuad.jobposting.domain.ApplicationDocument;
import com.thunder11.scuad.jobposting.dto.request.AiAnalysisRequest;
import com.thunder11.scuad.jobposting.dto.response.AiAnalysisResponse;
import com.thunder11.scuad.jobposting.dto.response.AiEvaluationResultResponse;
import com.thunder11.scuad.jobposting.dto.response.DocumentResponse;
import com.thunder11.scuad.jobposting.service.JobApplicationService;
import com.thunder11.scuad.jobposting.service.JopApplicationAnalysisService;

@RestController
@RequestMapping("/api/v1/applications")
@RequiredArgsConstructor
public class JobApplicationController {

    private final JobApplicationService jobApplicationService;
    private final JopApplicationAnalysisService jopApplicationAnalysisService;

    @GetMapping("/{applicationId}/analyses")
    public ApiResponse<AiEvaluationResultResponse> getAnalysisResult(
            @PathVariable("applicationId") Long applicationId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        AiEvaluationResultResponse result = jopApplicationAnalysisService.getAnalysisResult(principal.getUserId(), applicationId);

        return ApiResponse.of(200, "AI_EVALUATION_FOUND", "종합 평가 결과를 조회했습니다.", result);
    }

    @PostMapping("/{applicationId}/documents")
    public ApiResponse<DocumentResponse> uploadDocument(
            @PathVariable Long applicationId,
            @RequestParam("doc_type") String docType,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        ApplicationDocument savedDoc = jobApplicationService.uploadDocument(principal.getUserId(), applicationId, docType, file);
        DocumentResponse result = DocumentResponse.from(savedDoc);

        return ApiResponse.of(200, "DOCUMENT_UPLOAD_SUCCESS", "서류 업로드에 성공했습니다.", result);
    }

    @PostMapping("/{applicationId}/analyses")
    public ApiResponse<AiAnalysisResponse> requestAnalyses(
            @PathVariable("applicationId") Long applicationId,
            @RequestBody @Valid AiAnalysisRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Long jobId = jopApplicationAnalysisService.createEvaluationJob(applicationId, principal.getUserId(), request.getAnalysisType());

        AiAnalysisResponse result = AiAnalysisResponse.builder()
                .applicationId(applicationId)
                .evalJobId(jobId)
                .status("PENDING")
                .build();

        return ApiResponse.of(202, "AI_EVALUATION_ACCEPTED", "지원자 AI 종합평가가 요청되었습니다.", result);
    }


}
