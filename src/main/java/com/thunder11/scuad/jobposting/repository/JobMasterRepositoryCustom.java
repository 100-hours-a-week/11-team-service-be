package com.thunder11.scuad.jobposting.repository;

import java.util.List;

import com.thunder11.scuad.jobposting.domain.JobMaster;
import com.thunder11.scuad.jobposting.dto.request.JobPostingSearchCondition;

public interface JobMasterRepositoryCustom {
    List<JobMaster> searchJobPostings(JobPostingSearchCondition condition);
}
