package com.thunder11.scuad.jobposting.controller;

import java.util.Map;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

import com.thunder11.scuad.common.exception.ApiException;
import com.thunder11.scuad.common.exception.ErrorCode;
import com.thunder11.scuad.common.response.ApiResponse;
import com.thunder11.scuad.jobposting.dto.request.JobPostingConfirmRequest;
import com.thunder11.scuad.jobposting.dto.request.JobUrlAnalysisRequest;
import com.thunder11.scuad.jobposting.dto.response.JobAnalysisResultResponse;
import com.thunder11.scuad.jobposting.dto.response.JobPostingConfirmResponse;
import com.thunder11.scuad.jobposting.dto.response.JobPostingDetailResponse;
import com.thunder11.scuad.jobposting.service.JobPostingAnalysisService;
import com.thunder11.scuad.jobposting.service.JobPostingManagementService;
import com.thunder11.scuad.jobposting.dto.response.AiEvaluationResultResponse;
import com.thunder11.scuad.jobposting.service.JopApplicationAnalysisService;
import com.thunder11.scuad.jobposting.dto.request.JobPostingSearchCondition;
import com.thunder11.scuad.auth.security.UserPrincipal;

@RestController
@RequestMapping("/api/v1/job-postings")
@RequiredArgsConstructor
public class JobPostingController {

    private final JobPostingAnalysisService jobPostingAnalysisService;
    private final JobPostingManagementService jobPostingManagementService;
    private final JopApplicationAnalysisService jopApplicationAnalysisService;

    @GetMapping("/{jobMasterId}/my-application")
    public ApiResponse<AiEvaluationResultResponse> getMyApplication(
            @PathVariable("jobMasterId") Long jobMasterId,
            @AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            throw new ApiException(ErrorCode.UNAUTHORIZED, "인증 정보가 없습니다. 다시 로그인해 주세요.");
        }
        AiEvaluationResultResponse result = jopApplicationAnalysisService.getMyApplicationResult(
                principal.getUserId(),
                jobMasterId);

        return ApiResponse.of(200, "MY_APPLICATION_LOAD_SUCCESS", "내 지원 정보를 조회했습니다.", result);
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> getJobPostings(
            JobPostingSearchCondition condition) {
        Map<String, Object> result = jobPostingManagementService.getJobPostings(condition);
        return ApiResponse.of(200, "JOB_POST_LIST_LOAD_SUCCESS", "채용공고 목록 조회에 성공했습니다.", result);
    }

    @GetMapping("/{jobMasterId}")
    public ApiResponse<JobPostingDetailResponse> getJobPosting(
            @PathVariable("jobMasterId") long jobMasterId,
            @AuthenticationPrincipal UserPrincipal principal) {
        JobPostingDetailResponse result = jobPostingManagementService.getJobPostingDetail(jobMasterId);

        return ApiResponse.of(200, "JOB_POST_LOAD_SUCCESS", "공고 분석 결과 조회 성공", result);
    }

    @PostMapping
    public ApiResponse<JobAnalysisResultResponse> analyzeJobPosting(
            @Valid @RequestBody JobUrlAnalysisRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            throw new ApiException(ErrorCode.UNAUTHORIZED, "인증 정보가 없습니다.");
        }
        JobAnalysisResultResponse result = jobPostingAnalysisService.analyze(request.getUrl(), principal.getUserId());
        return ApiResponse.of(200, "JOB_ANALYSIS_SUCCESS", "채용공고 분석이 완료되었습니다.", result);
    }

    @PatchMapping("/{jobMasterId}")
    public ApiResponse<JobPostingConfirmResponse> confirmJobPosting(
            @PathVariable("jobMasterId") Long jobMasterId,
            @RequestBody @Valid JobPostingConfirmRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        JobPostingConfirmResponse result = jobPostingManagementService.confirmJobPosting(jobMasterId,
                principal.getUserId(), request.getRegistrationStatus());

        return ApiResponse.of(200, "JOB_MASTER_REGISTERED", "채용공고가 성공적으로 등록되었습니다.", result);
    }

    @DeleteMapping("/{jobMasterId}")
    public ResponseEntity<Void> deleteJobPosting(
            @PathVariable("jobMasterId") Long jobMasterId,
            @AuthenticationPrincipal UserPrincipal principal) {
        jobPostingManagementService.deleteJobPosting(jobMasterId, principal.getUserId());

        return ResponseEntity.noContent().build();
    }
}
