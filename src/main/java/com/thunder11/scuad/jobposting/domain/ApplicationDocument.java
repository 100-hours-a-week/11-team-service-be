package com.thunder11.scuad.jobposting.domain;

import java.time.LocalDateTime;

import jakarta.persistence.*;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import com.thunder11.scuad.common.entity.BaseTimeEntity;
import com.thunder11.scuad.file.domain.FileObject;
import com.thunder11.scuad.jobposting.domain.type.ApplicationDocumentType;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
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

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id", nullable = false)
    private FileObject file;

    @Enumerated(EnumType.STRING)
    @Column(name = "doc_type", nullable = false, length = 20)
    private ApplicationDocumentType docType;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
