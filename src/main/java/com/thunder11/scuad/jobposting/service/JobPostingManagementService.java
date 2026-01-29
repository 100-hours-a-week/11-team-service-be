package com.thunder11.scuad.jobposting.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import com.thunder11.scuad.common.exception.ApiException;
import com.thunder11.scuad.common.exception.ErrorCode;
import com.thunder11.scuad.jobposting.domain.JobPost;
import com.thunder11.scuad.jobposting.domain.type.RegistrationStatus;
import com.thunder11.scuad.jobposting.dto.response.JobPostingConfirmResponse;
import com.thunder11.scuad.jobposting.repository.JobPostRepository;
import com.thunder11.scuad.infra.ai.client.AiServiceClient;
import com.thunder11.scuad.jobposting.domain.JobMaster;
import com.thunder11.scuad.jobposting.dto.response.JobPostingDetailResponse;
import com.thunder11.scuad.jobposting.repository.JobMasterRepository;
import com.thunder11.scuad.jobposting.dto.request.JobPostingSearchCondition;
import com.thunder11.scuad.jobposting.dto.response.JobPostingListResponse;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JobPostingManagementService {

    private final JobPostRepository jobPostRepository;
    private final AiServiceClient aiServiceClient;
    private final JobMasterRepository jobMasterRepository;

    @Transactional(readOnly = true)
    public Map<String, Object> getJobPostings(JobPostingSearchCondition condition) {
        List<JobMaster> masters = jobMasterRepository.searchJobPostings(condition);

        List<JobPostingListResponse> items = masters.stream()
                .map(JobPostingListResponse::from)
                .toList();

        Long nextCursor = items.isEmpty() ? null : items.get(items.size() - 1).getId();

        boolean isLast = items.size() < condition.getSize();

        return Map.of(
                "items", items,
                "next_cursor", nextCursor != null ? nextCursor : -1L,
                "last", isLast);
    }

    @Transactional(readOnly = true)
    public JobPostingDetailResponse getJobPostingDetail(Long jobPostingId) {
        JobPost jobPost = jobPostRepository.findByIdAndDeletedAtIsNull(jobPostingId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "채용공고가 존재하지 않습니다."));

        JobMaster jobMaster = jobMasterRepository.findByIdWithDetails(jobPost.getJobMaster().getId())
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "채용공고 상세 정보를 찾을 수 없습니다."));

        return JobPostingDetailResponse.from(jobMaster);
    }

    @Transactional
    public JobPostingConfirmResponse confirmJobPosting(Long jobPostingId, Long userId, RegistrationStatus status) {
        JobPost jobPost = jobPostRepository.findByIdAndDeletedAtIsNull(jobPostingId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "채용공고가 존재하지 않습니다."));

        if (!jobPost.getCreatedBy().equals(userId)) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }

        if (status != RegistrationStatus.CONFIRMED) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "등록 확정은 CONFIRMED 상태로만 가능합니다");
        }

        if (jobPost.getRegistrationStatus() != RegistrationStatus.DRAFT) {
            throw new ApiException(ErrorCode.CONFLICT, "이미 확정되거나 취소된 공고입니다.");
        }

        jobPost.confirmRegistration();

        return new JobPostingConfirmResponse(
                jobPost.getId(),
                jobPost.getJobMaster().getId(),
                jobPost.getRegistrationStatus());
    }

    @Transactional
    public void deleteJobPosting(Long jobPostingId, Long userId) {
        JobPost jobPost = jobPostRepository.findByIdAndDeletedAtIsNull(jobPostingId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));

        if (!jobPost.getCreatedBy().equals(userId)) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }

        Long jobMasterId = jobPost.getJobMaster().getId();

        jobPostRepository.deleteHardById(jobPostingId);

        boolean hasRemainPosts = jobPostRepository.existsByJobMasterIdAndDeletedAtIsNull(jobMasterId);

        if (!hasRemainPosts) {
            jobMasterRepository.deleteHardById(jobMasterId);
        }

        aiServiceClient.deleteJobAnalysis(jobPost.getAiJobId());
    }
}
