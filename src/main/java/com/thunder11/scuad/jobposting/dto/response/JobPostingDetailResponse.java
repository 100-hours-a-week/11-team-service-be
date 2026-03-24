package com.thunder11.scuad.jobposting.dto.response;

import java.time.LocalDate;
import java.util.List;

import lombok.*;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.thunder11.scuad.jobposting.domain.JobMaster;
import com.thunder11.scuad.jobposting.domain.JobPost;
import com.thunder11.scuad.jobposting.domain.type.JobStatus;
import com.thunder11.scuad.jobposting.domain.type.RegistrationStatus;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class JobPostingDetailResponse {

        private Long jobMasterId;
        private Long jobPostingId;
        private JobStatus jobStatus;
        private String companyName;
        private String jobTitle;
        private RegistrationStatus registrationStatus;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        private LocalDate startDate;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        private LocalDate endDate;

        private List<String> mainTasks;
        private List<String> skills;
        private String aiSummary;
        private String sourceUrl;

        public static JobPostingDetailResponse from(JobMaster jobMaster) {
                List<String> techs = jobMaster.getJobMasterSkills().stream()
                                .map(jms -> jms.getSkill().getName())
                                .toList();

                JobPost primaryPost = jobMaster.getJobPosts().stream()
                                .filter(post -> post.getSourceUrl() != null && !post.getSourceUrl().isBlank())
                                .findFirst()
                                .orElse(null);

                String jobUrl = (primaryPost != null) ? primaryPost.getSourceUrl() : "";
                Long postId = (primaryPost != null) ? primaryPost.getId() : null;

                return JobPostingDetailResponse.builder()
                                .jobMasterId(jobMaster.getId())
                                .jobPostingId(postId)
                                .jobStatus(jobMaster.getStatus())
                                .companyName(jobMaster.getCompany().getName())
                                .jobTitle(jobMaster.getJobTitle())
                                .startDate(jobMaster.getStartDate())
                                .endDate(jobMaster.getEndDate())
                                .mainTasks(jobMaster.getMainTasks())
                                .skills(techs)
                                .aiSummary(jobMaster.getAiSummary())
                                .sourceUrl(jobUrl)
                                .registrationStatus((primaryPost != null) ? primaryPost.getRegistrationStatus() : null)
                                .build();
        }
}
