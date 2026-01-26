package com.thunder11.scuad.jobposting.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.thunder11.scuad.jobposting.domain.Company;

import java.util.Optional;

public interface CompanyRepository extends JpaRepository<Company, Long> {

    Optional<Company> findByName(String name);
}
