package com.thunder11.scuad.jobposting.dto.response;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Builder;
import lombok.Getter;

import com.thunder11.scuad.jobposting.domain.JobMaster;
import com.thunder11.scuad.jobposting.domain.type.JobStatus;




@Getter
@Builder
public class JobPostingListResponse {
    private Long id;
    private String companyName;
    private String jobTitle;
    private List<String> skills;
    private JobStatus status;

    @JsonFormat(shape =  JsonFormat.Shape.STRING, pattern ="yyyy.MM.dd")
    private LocalDate startDate;

    @JsonFormat(shape =  JsonFormat.Shape.STRING, pattern ="yyyy.MM.dd")
    private LocalDate endDate;

    private int currentGroupCount;

    public static JobPostingListResponse from(JobMaster jobMaster) {
        return JobPostingListResponse.builder()
                .id(jobMaster.getId())
                .companyName(jobMaster.getCompany().getName())
                .jobTitle(jobMaster.getJobTitle())
                .skills(jobMaster.getJobMasterSkills().stream()
                        .map(jms -> jms.getSkill().getName()).toList())
                .status(jobMaster.getStatus())
                .startDate(jobMaster.getStartDate())
                .endDate(jobMaster.getEndDate())
                .currentGroupCount(0) // 추후 chat 도메인 개발 후 변경
                .build();
    }
}
