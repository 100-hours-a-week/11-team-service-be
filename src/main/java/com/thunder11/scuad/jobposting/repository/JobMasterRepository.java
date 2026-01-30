package com.thunder11.scuad.jobposting.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.thunder11.scuad.jobposting.domain.JobMaster;

public interface JobMasterRepository extends JpaRepository<JobMaster, Long>, JobMasterRepositoryCustom {

    @Modifying
    @Query("DELETE FROM JobMaster j WHERE j.id = :id")
    void deleteHardById(@Param("id") long id);

    @Query("SELECT jm FROM JobMaster jm " +
            "JOIN FETCH jm.company " +
            "LEFT JOIN FETCH jm.jobMasterSkills jms " +
            "LEFT JOIN FETCH jms.skill " +
            "WHERE jm.id = :id AND jm.deletedAt IS NULL")
    Optional<JobMaster> findByIdWithDetails(@Param("id") Long id);

    // JobMaster 존재 여부 확인 (삭제되지 않은 것만)
    @Query("SELECT CASE WHEN COUNT(jm) > 0 THEN true ELSE false END " +
            "FROM JobMaster jm " +
            "WHERE jm.id = :jobMasterId " +
            "AND jm.deletedAt IS NULL")
    boolean existsByIdNotDeleted(@Param("jobMasterId") Long jobMasterId);
}
