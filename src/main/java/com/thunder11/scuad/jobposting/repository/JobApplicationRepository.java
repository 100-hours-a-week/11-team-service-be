package com.thunder11.scuad.jobposting.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.thunder11.scuad.jobposting.domain.JobApplication;

public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {
}
