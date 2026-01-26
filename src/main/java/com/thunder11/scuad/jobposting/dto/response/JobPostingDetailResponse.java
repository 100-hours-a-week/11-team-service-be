package com.thunder11.scuad.jobposting.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.thunder11.scuad.jobposting.domain.JobMaster;
import com.thunder11.scuad.jobposting.domain.type.JobStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class JobPostingDetailResponse {

    private Long jobMasterId;
    private JobStatus jobStatus;
    private String companyName;
    private String jobTitle;

    @JsonFormat(shape =  JsonFormat.Shape.STRING, pattern ="yyyy-MM-dd")
    private LocalDate startDate;

    @JsonFormat(shape =  JsonFormat.Shape.STRING, pattern ="yyyy-MM-dd")
    private LocalDate endDate;

    private List<String> mainTasks;
    private List<String> skills;
    private String aiSummary;
    private String sourceUrl;

    public static JobPostingDetailResponse from(JobMaster jobMaster) {
        List<String> techs = jobMaster.getJobMasterSkills().stream()
                .map(jms -> jms.getSkill().getName())
                .toList();

        String jobUrl = jobMaster.getJobPosts().isEmpty()
                ? "" : jobMaster.getJobPosts().get(0).getSourceUrl();

        return JobPostingDetailResponse.builder()
                .jobMasterId(jobMaster.getId())
                .jobStatus(jobMaster.getStatus())
                .companyName(jobMaster.getCompany().getName())
                .jobTitle(jobMaster.getJobTitle())
                .startDate(jobMaster.getStartDate())
                .endDate(jobMaster.getEndDate())
                .mainTasks(jobMaster.getMainTasks())
                .skills(techs)
                .aiSummary(jobMaster.getAiSummary())
                .sourceUrl(jobUrl)
                .build();
    }
}
