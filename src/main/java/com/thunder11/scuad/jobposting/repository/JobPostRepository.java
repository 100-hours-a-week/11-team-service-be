package com.thunder11.scuad.jobposting.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.thunder11.scuad.jobposting.domain.JobPost;
import com.thunder11.scuad.jobposting.domain.type.RegistrationStatus;

public interface JobPostRepository extends JpaRepository<JobPost, Long> {

    Optional<JobPost> findByIdAndDeletedAtIsNull(Long id);

    Optional<JobPost> findBySourceUrlHashAndDeletedAtIsNull(String sourceUrlHash);

    Optional<JobPost> findByFingerprintHashAndDeletedAtIsNull(String fingerprintHash);

    boolean existsBySourceUrlHashAndDeletedAtIsNull(String sourceUrlHash);

    boolean existsByFingerprintHashAndDeletedAtIsNull(String fingerprintHash);

    boolean existsByJobMasterIdAndDeletedAtIsNull(Long jobMasterId);

    @Modifying
    @Query("DELETE FROM JobPost j WHERE j.id = :id")
    void deleteHardById(@Param("id") long id);
}
