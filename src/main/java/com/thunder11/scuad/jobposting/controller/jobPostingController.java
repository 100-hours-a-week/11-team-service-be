package com.thunder11.scuad.jobposting.controller;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

import com.thunder11.scuad.common.response.ApiResponse;
import com.thunder11.scuad.jobposting.dto.request.JobUrlAnalysisRequest;
import com.thunder11.scuad.jobposting.dto.response.JobAnalysisResultResponse;
import com.thunder11.scuad.jobposting.service.JobPostingAnalysisService;

@RestController
@RequestMapping("/api/v1/job-postings")
@RequiredArgsConstructor
public class jobPostingController {

    private final JobPostingAnalysisService jobPostingAnalysisService;

    @PostMapping
    public ApiResponse<JobAnalysisResultResponse> analyzeJobPosting(
            @Valid @RequestBody JobUrlAnalysisRequest request,
            @RequestParam(value = "userId", required = false, defaultValue = "1") Long userId // 임시
    ) {
        JobAnalysisResultResponse result = jobPostingAnalysisService.analyze(request.getUrl(),  userId);
        return ApiResponse.of(200, "JOB_ANALYSIS_SUCCESS","채용공고 분석이 완료되었습니다.", result);
    }
}
