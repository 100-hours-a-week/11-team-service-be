package com.thunder11.scuad.jobposting.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.thunder11.scuad.jobposting.domain.ApplicationDocument;
import com.thunder11.scuad.jobposting.domain.type.ApplicationDocumentType;

public interface ApplicationDocumentRepository extends JpaRepository<ApplicationDocument, Long> {
    boolean existsByJobApplicationIdAndDocType(long jobApplicationId, ApplicationDocumentType docType);
}
