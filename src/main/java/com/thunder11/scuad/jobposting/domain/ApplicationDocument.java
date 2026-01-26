package com.thunder11.scuad.jobposting.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import com.thunder11.scuad.jobposting.domain.type.ApplicationDocumentType;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "application_documents",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_application_documents_job_application_id_doc_type",
                        columnNames = { "job_application_id", "doc_type" }
                )
        }
)
@SQLDelete(sql = "UPDATE application_documents SET deleted_at = CURRENT_TIMESTAMP WHERE application_document_id = ?")
@SQLRestriction("deleted_at IS NULL")
public class ApplicationDocument extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "application_document_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_application_id", nullable = false)
    private JobApplication jobApplication;

    @Column(name = "file_id", nullable = false)
    private Long fileId;

    @Enumerated(EnumType.STRING)
    @Column(name = "doc_type", nullable = false, length = 20)
    private ApplicationDocumentType docType;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
