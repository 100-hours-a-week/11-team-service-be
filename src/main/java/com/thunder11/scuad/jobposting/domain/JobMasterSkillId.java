package com.thunder11.scuad.jobposting.domain;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class JobMasterSkillId implements Serializable {

    @Column(name = "job_master_id")
    private Long jobMasterId;

    @Column(name = "skill_id")
    private Long skillId;

    public JobMasterSkillId(Long jobMasterId, Long skillId) {
        this.jobMasterId = jobMasterId;
        this.skillId = skillId;
    }
}
