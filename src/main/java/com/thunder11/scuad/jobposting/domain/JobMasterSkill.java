package com.thunder11.scuad.jobposting.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import org.hibernate.annotations.SQLRestriction;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(
        name = "job_master_skills",
        indexes = {
                @Index(name = "idx_job_master_skills_skill_id", columnList = "skill_id")
        }
)
@SQLRestriction("deleted_at IS NULL")
@EntityListeners(AuditingEntityListener.class)
public class JobMasterSkill {

    @EmbeddedId
    private JobMasterSkillId id;

    @MapsId("jobMasterId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_master_id", nullable = false)
    private JobMaster jobMaster;

    @MapsId("skillId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "skill_id", nullable = false)
    private Skill skill;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
