package com.thunder11.scuad.jobposting.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.thunder11.scuad.jobposting.domain.Skill;

public interface SkillRepository extends JpaRepository<Skill, Long> {

    Optional<Skill> findByName(String name);
}
