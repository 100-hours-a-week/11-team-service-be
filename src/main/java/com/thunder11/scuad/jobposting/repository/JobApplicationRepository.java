package com.thunder11.scuad.jobposting.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.thunder11.scuad.jobposting.domain.JobApplication;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {
    // 특정 사용자의 특정 공고 지원서 조회
    @Query("SELECT ja FROM JobApplication ja " +
            "WHERE ja.user.userId = :userId " +
            "AND ja.jobMaster.id = :jobMasterId " +
            "AND ja.deletedAt IS NULL")
    Optional<JobApplication> findByUserIdAndJobMasterId(
            @Param("userId") Long userId,
            @Param("jobMasterId") Long jobMasterId
    );
}
