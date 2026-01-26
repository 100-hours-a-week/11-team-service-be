package com.thunder11.scuad.jobposting.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.thunder11.scuad.jobposting.domain.JobMaster;
import com.thunder11.scuad.jobposting.domain.type.JobStatus;

public interface JobMasterRepository extends JpaRepository<JobMaster, Long> {

    List<JobMaster> findByStatusOrderByEndDateAsc(JobStatus status);

    List<JobMaster> findByCompanyIdAndStatusOrderByEndDateAsc(Long companyId, JobStatus status);

    @Modifying
    @Query("DELETE FROM JobMaster j WHERE j.id = :id")
    void deleteHardById(@Param("id") long id);

    @Query("SELECT jm FROM JobMaster jm " +
            "JOIN FETCH jm.company " +
            "LEFT JOIN FETCH jm.jobMasterSkills jms " +
            "LEFT JOIN FETCH jms.skill " +
            "WHERE jm.id = :id AND jm.deletedAt IS NULL")
    Optional<JobMaster> findByIdWithDetails(@Param("id") Long id);
}
