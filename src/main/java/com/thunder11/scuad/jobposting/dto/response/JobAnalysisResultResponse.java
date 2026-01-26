package com.thunder11.scuad.jobposting.dto.response;

import java.time.LocalDate;
import java.util.List;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class JobAnalysisResultResponse {

    private Long jobMasterId;
    private Long jobPostingId;

    private boolean isExisting;

    private String companyName;
    private String jobTitle;

    private List<String> mainTasks;
    private List<String> skills;

    private String status;
    private LocalDate startDate;
    private LocalDate endDate;
    private String aiSummary;
}
