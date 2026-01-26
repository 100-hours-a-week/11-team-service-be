package com.thunder11.scuad.jobposting.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.thunder11.scuad.jobposting.domain.JobMaster;
import com.thunder11.scuad.jobposting.domain.type.JobStatus;

public interface JobMasterRepository extends JpaRepository<JobMaster, Long> {

    List<JobMaster> findByStatusOrderByEndDateAsc(JobStatus status);

    List<JobMaster> findByCompanyIdAndStatusOrderByEndDateAsc(Long companyId, JobStatus status);
}
