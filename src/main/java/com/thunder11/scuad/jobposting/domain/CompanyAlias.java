package com.thunder11.scuad.jobposting.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import com.thunder11.scuad.common.entity.BaseTimeEntity;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "company_aliases",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_company_aliases_company_norm",
                        columnNames = { "company_id", "alias_normalized" }
                )
        }
)
@SQLDelete(sql = "UPDATE company_aliases SET deleted_at = CURRENT_TIMESTAMP WHERE alias_id = ?")
@SQLRestriction("deleted_at IS NULL")
public class CompanyAlias extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "alias_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "source", nullable = false, length = 30)
    private String source;

    @Column(name = "alias_name", nullable = false, length = 150)
    private String aliasName;

    @Column(name = "alias_normalized", nullable = false, length = 150)
    private String aliasNormalized;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public CompanyAlias(Company company, String source, String aliasName, String aliasNormalized) {
        this.company = company;
        this.source = source;
        this.aliasName = aliasName;
        this.aliasNormalized = aliasNormalized;
    }
}
