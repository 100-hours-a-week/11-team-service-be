package com.thunder11.scuad.jobposting.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
    Optional<JobApplication> findByUserUserIdAndJobMasterId(Long userId, Long jobMasterId);

    @Query("SELECT ja FROM JobApplication ja LEFT JOIN FETCH ja.applicationDocuments WHERE ja.id = :id")
    Optional<JobApplication> findByIdWithDocuments(@Param("id") Long id);
}
