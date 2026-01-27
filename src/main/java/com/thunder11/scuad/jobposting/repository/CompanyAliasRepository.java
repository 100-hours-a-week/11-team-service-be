package com.thunder11.scuad.jobposting.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.thunder11.scuad.jobposting.domain.Company;
import com.thunder11.scuad.jobposting.domain.CompanyAlias;

public interface CompanyAliasRepository extends JpaRepository<CompanyAlias, Long> {

    @Query("""
            select ca.company
            from CompanyAlias ca
            where ca.aliasNormalized = :aliasNormalized
              and ca.deletedAt is null
              and ca.company.deletedAt is null
            """)
    Optional<Company> findCompanyByAliasNormalized(@Param("aliasNormalized") String aliasNormalized);

    boolean existsByCompanyAndSourceAndAliasName(Company company, String source, String aliasName);
    boolean existsByCompanyAndAliasNormalized(Company company, String aliasNormalized);
}
