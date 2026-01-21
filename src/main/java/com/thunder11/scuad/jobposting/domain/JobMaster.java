package com.thunder11.scuad.jobposting.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.thunder11.scuad.common.entity.BaseTimeEntity;
import com.thunder11.scuad.jobposting.domain.type.JobStatus;
import jakarta.persistence.*;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "job_masters", indexes = {
        @Index(name = "idx_job_masters_open_enddate", columnList = "status, end_date, job_master_id, company_id, job_title, start_date"),
        @Index(name = "idx_job_masters_company_open_enddate", columnList = "company_id, status, end_date, job_master_id, job_title, start_date")
})
@SQLDelete(sql = "UPDATE job_masters SET deleted_at = CURRENT_TIMESTAMP WHERE job_master_id = ?")
@SQLRestriction("deleted_at IS NULL")
public class JobMaster extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "job_master_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "job_title", nullable = false, length = 150)
    private String jobTitle;

    @Column(name = "main_tasks", columnDefinition = "json")
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> mainTasks;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "ai_summary", columnDefinition = "TEXT")
    private String aiSummary;

    @Column(name = "evaluation_criteria", columnDefinition = "json")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> evaluationCriteria;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private JobStatus status;

    @Column(name = "last_seen_at")
    private LocalDateTime lastSeenAt;

    @Builder.Default
    @OneToMany(mappedBy = "jobMaster")
    private List<JobPost> jobPosts = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "jobMaster")
    private List<JobApplication> jobApplications = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "jobMaster")
    private List<JobMasterSkill> jobMasterSkills = new ArrayList<>();

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
