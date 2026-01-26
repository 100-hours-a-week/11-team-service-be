package com.thunder11.scuad.jobposting.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.thunder11.scuad.jobposting.domain.JobPost;

public interface JobPostRepository extends JpaRepository<JobPost, Long> {

    Optional<JobPost> findBySourceUrlHashAndDeletedAtIsNull(String sourceUrlHash);

    Optional<JobPost> findByFingerprintHashAndDeletedAtIsNull(String fingerprintHash);

    boolean existsBySourceUrlHashAndDeletedAtIsNull(String sourceUrlHash);

    boolean existsByFingerprintHashAndDeletedAtIsNull(String fingerprintHash);

}
