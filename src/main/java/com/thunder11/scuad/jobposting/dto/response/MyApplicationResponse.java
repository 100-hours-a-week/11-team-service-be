package com.thunder11.scuad.jobposting.dto.response;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;

import com.thunder11.scuad.jobposting.domain.JobApplication;

@Getter
@Builder
public class MyApplicationResponse {
    private Long id;
    private Long jobMasterId;
    private String companyName;
    private String jobTitle;
    private LocalDateTime appliedAt;

    public static MyApplicationResponse from (JobApplication ja) {
        return MyApplicationResponse.builder()
                .id(ja.getId())
                .jobMasterId(ja.getJobMaster().getId())
                .companyName(ja.getJobMaster().getCompany().getName())
                .jobTitle(ja.getJobMaster().getJobTitle())
                .appliedAt(ja.getCreatedAt())
                .build();
    }
}