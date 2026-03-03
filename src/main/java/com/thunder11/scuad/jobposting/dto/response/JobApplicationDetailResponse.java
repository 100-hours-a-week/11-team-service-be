package com.thunder11.scuad.jobposting.dto.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.thunder11.scuad.jobposting.domain.JobApplication;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class JobApplicationDetailResponse {
    private Long applicationId;
    private Long jobMasterId;

    private String companyName;
    private String jobTitle;
    private List<ApplicationDocumentResponse> documents;

    public static JobApplicationDetailResponse of(JobApplication application,
            List<ApplicationDocumentResponse> documents) {
        return JobApplicationDetailResponse.builder()
                .applicationId(application.getId())
                .jobMasterId(application.getJobMaster().getId())
                .companyName(application.getJobMaster().getCompany().getName()) // [추가]
                .jobTitle(application.getJobMaster().getJobTitle())
                .documents(documents)
                .build();
    }

    @Getter
    @Builder
    public static class ApplicationDocumentResponse {
        private String docType;
        @JsonProperty("isRegistered")
        private boolean isRegistered;
        private String originalFileName;
        private String fileUrl;
    }
}