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
@Table(name = "companies")
@SQLDelete(sql = "UPDATE companies SET deleted_at = CURRENT_TIMESTAMP WHERE company_id = ?")
@SQLRestriction("deleted_at IS NULL")
public class Company extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "company_id")
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "domain", length = 100)
    private String domain;

    @OneToMany(mappedBy = "company")
    private List<CompanyAlias> aliases = new ArrayList<>();

    @OneToMany(mappedBy = "company")
    private List<JobMaster> jobMasters = new ArrayList<>();

    @OneToMany(mappedBy = "company")
    private List<JobPost> jobPosts = new ArrayList<>();

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public Company(String name, String domain) {
        this.name = name;
        this.domain = domain;
    }
}
