package com.thunder11.scuad.jobposting.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;

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
import com.thunder11.scuad.jobposting.repository.JobMasterSkillRepository;
import com.thunder11.scuad.chat.repository.ChatRoomRepository;
import com.thunder11.scuad.common.util.CursorTokenUtil;
import com.thunder11.scuad.jobposting.dto.response.JobPostingSliceResponse;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JobPostingManagementService {

    private final JobPostRepository jobPostRepository;
    private final AiServiceClient aiServiceClient;
    private final JobMasterRepository jobMasterRepository;
    private final JobMasterSkillRepository jobMasterSkillRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final EntityManager entityManager;
    private final CursorTokenUtil cursorTokenUtil;

    @Transactional(readOnly = true)
    @Cacheable(value = "jobPostings", key = "#condition.toString() + '_' + #condition.getSize()", cacheManager = "cacheManager")
    public JobPostingSliceResponse getJobPostings(JobPostingSearchCondition condition) {
        CursorTokenUtil.CursorData cursorData = null;
        if (condition.getCursor() != null && !condition.getCursor().isBlank() && !"-1".equals(condition.getCursor())) {
            cursorData = cursorTokenUtil.decodeToken(condition.getCursor());
        }

        List<JobMaster> masters = jobMasterRepository.searchJobPostings(condition, cursorData);

        List<Long> masterIds = masters.stream().map(JobMaster::getId).toList();
        Map<Long, Long> chatCounts = chatRoomRepository.countActiveRoomsByJobMasterIds(masterIds)
                .stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]));

        List<JobPostingListResponse> items = masters.stream()
                .map(m -> JobPostingListResponse.of(m, chatCounts.getOrDefault(m.getId(), 0L).intValue()))
                .toList();

        String nextCursorToken = "";
        if (!masters.isEmpty()) {
            JobMaster lastItem = masters.get(masters.size() - 1);
            String token = cursorTokenUtil.createToken(lastItem.getId(), lastItem.getEndDate(), lastItem.getStatus());
            nextCursorToken = (token != null) ? token : "";
        }

        boolean isLast = items.size() < condition.getSize();

        return JobPostingSliceResponse.of(items, nextCursorToken, isLast);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "jobDetail", key = "#jobMasterId", cacheManager = "cacheManager")
    public JobPostingDetailResponse getJobPostingDetail(Long jobMasterId) {
        JobMaster jobMaster = jobMasterRepository.findByIdWithDetails(jobMasterId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "채용공고 상세 정보를 찾을 수 없습니다."));

        return JobPostingDetailResponse.from(jobMaster);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "jobPostings", allEntries = true),
            @CacheEvict(value = "jobDetail", key = "#jobMasterId")
    })
    public JobPostingConfirmResponse confirmJobPosting(Long jobMasterId, Long userId, RegistrationStatus status) {
        JobMaster jobMaster = jobMasterRepository.findById(jobMasterId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "채용공고를 찾을 수 없습니다."));

        JobPost jobPost = jobMaster.getJobPosts().stream()
                .filter(p -> p.getRegistrationStatus() == RegistrationStatus.DRAFT)
                .findFirst()
                .orElseThrow(() -> new ApiException(ErrorCode.CONFLICT, "확정할 수 있는 대기 상태의 공고가 없습니다."));

        if (!jobPost.getCreatedBy().equals(userId)) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }

        if (status != RegistrationStatus.CONFIRMED) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "등록 확정은 CONFIRMED 상태로만 가능합니다");
        }

        jobPost.confirmRegistration();

        return new JobPostingConfirmResponse(
                jobPost.getId(),
                jobMaster.getId(),
                jobPost.getRegistrationStatus());
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "jobPostings", allEntries = true),
            @CacheEvict(value = "jobDetail", key = "#jobMasterId")
    })
    public void deleteJobPosting(Long jobMasterId, Long userId) {
        JobMaster jobMaster = jobMasterRepository.findById(jobMasterId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "삭제할 공고를 찾을 수 없습니다."));

        boolean isOwner = jobMaster.getJobPosts().stream()
                .anyMatch(p -> p.getCreatedBy().equals(userId));

        if (!isOwner) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }

        aiServiceClient.deleteJobAnalysis(jobMaster.getJobPosts().get(0).getAiJobId());

        entityManager.detach(jobMaster);
        jobMasterSkillRepository.deleteHardByJobMasterId(jobMasterId);

        jobMaster.getJobPosts().forEach(p -> jobPostRepository.deleteHardById(p.getId()));

        jobMasterRepository.deleteHardById(jobMasterId);
    }
}
