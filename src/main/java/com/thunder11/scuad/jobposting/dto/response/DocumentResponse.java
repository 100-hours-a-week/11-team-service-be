package com.thunder11.scuad.jobposting.dto.response;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;

import com.thunder11.scuad.jobposting.domain.ApplicationDocument;

@Getter
@Builder
public class DocumentResponse {

    private Long applicationDocumentId;
    private Long jobApplicationId;
    private String docType;

    private Long fileId;
    private String fileName;
    private Long fileSize;
    private String contentType;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static DocumentResponse from(ApplicationDocument document) {
        return DocumentResponse.builder()
                .applicationDocumentId(document.getId())
                .jobApplicationId(document.getJobApplication().getId())
                .docType(document.getDocType().name())
                .fileId(document.getFile().getId())
                .fileName(document.getFile().getOriginalName())
                .fileSize(document.getFile().getSizeBytes())
                .contentType(document.getFile().getContentType())
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .build();
    }
}