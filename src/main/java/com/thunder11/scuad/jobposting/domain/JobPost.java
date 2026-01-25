package com.thunder11.scuad.jobposting.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import com.thunder11.scuad.common.entity.BaseTimeEntity;
import com.thunder11.scuad.jobposting.domain.type.JobSourceType;
import com.thunder11.scuad.jobposting.domain.type.RecruitmentStatus;
import com.thunder11.scuad.jobposting.domain.type.RegistrationStatus;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(
        name = "job_posts",
        indexes = {
                @Index(name = "idx_job_posts_job_master_id", columnList = "job_master_id")
        }
)
@SQLDelete(sql = "UPDATE job_posts SET deleted_at = CURRENT_TIMESTAMP WHERE job_post_id = ?")
@SQLRestriction("deleted_at IS NULL")
public class JobPost extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "job_post_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_master_id", nullable = false)
    private JobMaster jobMaster;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "created_by")
    private Long createdBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 20)
    private JobSourceType sourceType;

    @Column(name = "source_url", nullable = false, length = 500, unique = true)
    private String sourceUrl;

    @Column(name = "source_url_hash", nullable = false, length = 64)
    private String sourceUrlHash;

    @Column(name = "raw_company_name", length = 100)
    private String rawCompanyName;

    @Column(name = "raw_job_title", length = 150)
    private String rawJobTitle;

    @Column(name = "main_tasks", columnDefinition = "json")
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> mainTasks;

    @Enumerated(EnumType.STRING)
    @Column(name = "recruitment_status", nullable = false, length = 20)
    private RecruitmentStatus recruitmentStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "registration_status", nullable = false, length = 20)
    private RegistrationStatus registrationStatus;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "fingerprint_hash", nullable = false, length = 64)
    private String fingerprintHash;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public void confirmRegistration() {
        this.registrationStatus = RegistrationStatus.CONFIRMED;

    }
}
