package com.thunder11.scuad.jobposting.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.thunder11.scuad.common.entity.BaseTimeEntity;
import jakarta.persistence.*;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "skills")
@SQLDelete(sql = "UPDATE skills SET deleted_at = NOW() WHERE skill_id = ?")
@SQLRestriction("deleted_at IS NULL")
public class Skill extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "skill_id")
    private Long id;

    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    @OneToMany(mappedBy = "skill")
    private List<SkillAlias> skillAliases = new ArrayList<>();

    @OneToMany(mappedBy = "skill")
    private List<JobMasterSkill> jobMasterSkills = new ArrayList<>();

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
