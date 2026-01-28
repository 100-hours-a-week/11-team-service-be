package com.thunder11.scuad.jobposting.service;

import com.thunder11.scuad.common.exception.ApiException;
import com.thunder11.scuad.common.exception.ErrorCode;
import com.thunder11.scuad.file.domain.FileObject;
import com.thunder11.scuad.file.service.FileStorageService;
import com.thunder11.scuad.jobposting.domain.ApplicationDocument;
import com.thunder11.scuad.jobposting.domain.JobApplication;
import com.thunder11.scuad.jobposting.domain.type.ApplicationDocumentType;
import com.thunder11.scuad.jobposting.repository.ApplicationDocumentRepository;
import com.thunder11.scuad.jobposting.repository.JobApplicationRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class JobApplicationService {

    private final JobApplicationRepository jobApplicationRepository;
    private final ApplicationDocumentRepository applicationDocumentRepository;
    private final FileStorageService fileStorageService;

    @Transactional
    public ApplicationDocument uploadDocument(Long applicationId, String docType, MultipartFile file) {
        if("RESUME".equalsIgnoreCase(docType) || "PORTFOLIO".equalsIgnoreCase(docType)) {
            if (!"application/pdf".equals(file.getContentType())) {
                throw new ApiException(ErrorCode.INVALID_REQUEST, "PDF 파일만 업로드 가능합니다.");
            }
        } else {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "문서 타입은 RESUME 또는 PORTFOLIO여야 합니다.");
        }

        ApplicationDocumentType type = ApplicationDocumentType.valueOf(docType.toUpperCase());
        if (applicationDocumentRepository.existsByJobApplicationIdAndDocType(applicationId, type)) {
            throw new ApiException(ErrorCode.CONFLICT, "이미 해당 타입의 문서가 등록되어있습니다.");
        }

        JobApplication jobApplication = jobApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "지원공고를 찾을 수 없습니다."));

        String uploadPath = "applications/" + applicationId + "/" + docType.toLowerCase();
        FileObject savedFile = fileStorageService.uploadFile(file, uploadPath);

        ApplicationDocument applicationDocument =
                ApplicationDocument.builder()
                        .jobApplication(jobApplication)
                        .file(savedFile)
                        .docType(ApplicationDocumentType.valueOf(docType.toUpperCase()))
                        .build();

        return applicationDocumentRepository.save(applicationDocument);
        }
    }


