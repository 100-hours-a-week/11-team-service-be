package com.thunder11.scuad.jobposting.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import com.thunder11.scuad.auth.domain.User;
import com.thunder11.scuad.common.entity.BaseTimeEntity;
import com.thunder11.scuad.jobposting.domain.type.ApplicationStatus;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "job_applications",
        indexes = {
                @Index(name = "idx_job_applications_job_master_id", columnList = "job_master_id")
        }
)
@SQLDelete(sql = "UPDATE job_applications SET deleted_at = CURRENT_TIMESTAMP WHERE job_application_id = ?")
@SQLRestriction("deleted_at IS NULL")
public class JobApplication extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "job_application_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_master_id", nullable = false)
    private JobMaster jobMaster;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ApplicationStatus status;

    @OneToMany(mappedBy = "jobApplication")
    private List<ApplicationDocument> applicationDocuments = new ArrayList<>();

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
