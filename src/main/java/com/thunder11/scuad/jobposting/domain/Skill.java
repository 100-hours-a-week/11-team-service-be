package com.thunder11.scuad.jobposting.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import com.thunder11.scuad.common.entity.BaseTimeEntity;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "skills")
@SQLDelete(sql = "UPDATE skills SET deleted_at = NOW() WHERE skill_id = ?")
@SQLRestriction("deleted_at IS NULL")
public class Skill extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "skill_id")
    private Long id;

    @Column(name = "skill_name", nullable = false, unique = true, length = 100)
    private String name;

    @Builder.Default
    @OneToMany(mappedBy = "skill")
    private List<SkillAlias> skillAliases = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "skill")
    private List<JobMasterSkill> jobMasterSkills = new ArrayList<>();

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
