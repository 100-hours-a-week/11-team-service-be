package com.thunder11.scuad.jobposting.domain;

import java.time.LocalDateTime;

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
@Table(name = "skill_aliases")
@SQLDelete(sql = "UPDATE skill_aliases SET deleted_at = CURRENT_TIMESTAMP WHERE alias_id = ?")
@SQLRestriction("deleted_at IS NULL")
public class SkillAlias extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "alias_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "skill_id", nullable = false)
    private Skill skill;

    @Column(name = "alias_name", nullable = false, length = 100)
    private String aliasName;

    @Column(name = "alias_normalized", nullable = false, length = 100)
    private String aliasNormalized;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
