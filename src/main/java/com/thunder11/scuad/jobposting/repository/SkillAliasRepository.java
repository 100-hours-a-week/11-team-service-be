package com.thunder11.scuad.jobposting.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.thunder11.scuad.jobposting.domain.Skill;
import com.thunder11.scuad.jobposting.domain.SkillAlias;

public interface SkillAliasRepository extends JpaRepository<SkillAlias, Long> {

    @Query("""
           select sa.skill
           from SkillAlias sa
           where sa.aliasNormalized = :aliasNormalized
           """)
    Optional<Skill> findSkillByAliasNormalized(String aliasNormalized);
}
