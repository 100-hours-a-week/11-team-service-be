package com.thunder11.scuad.jobposting.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.thunder11.scuad.jobposting.domain.JobMasterSkill;
import com.thunder11.scuad.jobposting.domain.JobMasterSkillId;

public interface JobMasterSkillRepository extends JpaRepository<JobMasterSkill, JobMasterSkillId> {

    @Modifying
    @Query("DELETE FROM JobMasterSkill jms WHERE jms.id.jobMasterId = :jobMasterId")
    void deleteHardByJobMasterId(@Param("jobMasterId") Long jobMasterId);
}
