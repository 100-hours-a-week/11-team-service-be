package com.thunder11.scuad.jobposting.service;

import com.thunder11.scuad.jobposting.repository.JobMasterRepository;
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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JobPostingManagementService {

    private final JobPostRepository jobPostRepository;
    private final AiServiceClient aiServiceClient;
    private final JobMasterRepository jobMasterRepository;

    @Transactional
    public JobPostingConfirmResponse confirmJobPosting(Long jobPostingId, Long userId, RegistrationStatus status) {
        JobPost jobPost = jobPostRepository.findByIdAndDeletedAtIsNull(jobPostingId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "채용공고를 조회할 수 없습니다."));

        if(!jobPost.getCreatedBy().equals(userId)) {
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
                jobPost.getRegistrationStatus()
        );
    }

    @Transactional
    public void deleteJobPosting(Long jobPostingId, Long userId) {
        JobPost jobPost = jobPostRepository.findByIdAndDeletedAtIsNull(jobPostingId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));

        if(!jobPost.getCreatedBy().equals(userId)) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }

        Long jobMasterId = jobPost.getJobMaster().getId();

        jobPostRepository.deleteHardById(jobPostingId);

        boolean hasRemainPosts = jobPostRepository.existsByJobMasterIdAndDeletedAtIsNull(jobMasterId);

        if(!hasRemainPosts) {
            jobMasterRepository.deleteHardById(jobMasterId);
        }

        aiServiceClient.deleteJobAnalysis(jobPost.getAiJobId());
    }
}
