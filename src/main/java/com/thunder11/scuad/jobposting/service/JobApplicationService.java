package com.thunder11.scuad.jobposting.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

import lombok.RequiredArgsConstructor;

import com.thunder11.scuad.common.exception.ApiException;
import com.thunder11.scuad.common.exception.ErrorCode;
import com.thunder11.scuad.file.domain.FileObject;
import com.thunder11.scuad.file.service.FileStorageService;
import com.thunder11.scuad.jobposting.domain.ApplicationDocument;
import com.thunder11.scuad.jobposting.domain.JobApplication;
import com.thunder11.scuad.jobposting.domain.type.ApplicationDocumentType;
import com.thunder11.scuad.jobposting.repository.ApplicationDocumentRepository;
import com.thunder11.scuad.jobposting.repository.JobApplicationRepository;
import com.thunder11.scuad.auth.domain.User;
import com.thunder11.scuad.auth.repository.UserRepository;
import com.thunder11.scuad.jobposting.domain.JobMaster;
import com.thunder11.scuad.jobposting.domain.type.ApplicationStatus;
import com.thunder11.scuad.jobposting.repository.JobMasterRepository;

@Service
@RequiredArgsConstructor
public class JobApplicationService {

    private final UserRepository userRepository;
    private final JobMasterRepository jobMasterRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final ApplicationDocumentRepository applicationDocumentRepository;
    private final FileStorageService fileStorageService;
    private final JopApplicationAnalysisService analysisService;

    @Transactional
    public Long apply(Long userId, Long jobMasterId, MultipartFile resume, MultipartFile portfolio) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "사용자를 찾을 수 없습니다."));

        JobMaster jobMaster = jobMasterRepository.findById(jobMasterId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "채용공고를 찾을 수 없습니다."));

        Optional<JobApplication> existingApplication = jobApplicationRepository.findByUserUserIdAndJobMasterId(userId,
                jobMasterId);
        if (existingApplication.isPresent()) {
            return existingApplication.get().getId();
        }

        JobApplication application = JobApplication.builder()
                .user(user)
                .jobMaster(jobMaster)
                .status(ApplicationStatus.ACTIVE)
                .build();

        jobApplicationRepository.save(application);

        saveDocument(application, "RESUME", resume);

        if (portfolio != null && !portfolio.isEmpty()) {
            saveDocument(application, "PORTFOLIO", portfolio);
        }

        analysisService.createEvaluationJob(application.getId(), userId, "EVALUATION");

        return application.getId();
    }

    @Transactional
    public ApplicationDocument uploadDocument(Long userId, Long applicationId, String docType, MultipartFile file) {

        JobApplication jobApplication = jobApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "지원공고를 찾을 수 없습니다."));

        if (!jobApplication.getUser().getUserId().equals(userId)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "본인의 지원서에만 파일을 업로드할 수 있습니다.");
        }

        if ("RESUME".equalsIgnoreCase(docType) || "PORTFOLIO".equalsIgnoreCase(docType)) {
            validateFile(file, docType);
        } else {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "문서 타입은 RESUME 또는 PORTFOLIO여야 합니다.");
        }

        ApplicationDocumentType type = ApplicationDocumentType.valueOf(docType.toUpperCase());
        if (applicationDocumentRepository.existsByJobApplicationIdAndDocType(applicationId, type)) {
            throw new ApiException(ErrorCode.CONFLICT, "이미 해당 타입의 문서가 등록되어있습니다.");
        }

        return saveDocument(jobApplication, docType, file);
    }

    private void validateFile(MultipartFile file, String docType) {

        if (!"application/pdf".equals(file.getContentType())) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "PDF 파일만 업로드 가능합니다.");
        }
    }

    private ApplicationDocument saveDocument(JobApplication application, String docType, MultipartFile file) {

        String uploadPath = "applications/" + application.getId() + "/" + docType.toLowerCase();
        FileObject savedFile = fileStorageService.uploadFile(file, uploadPath);

        ApplicationDocument document = ApplicationDocument.builder()
                .jobApplication(application)
                .file(savedFile)
                .docType(ApplicationDocumentType.valueOf(docType.toUpperCase()))
                .build();

        application.addApplicationDocument(document);
        return applicationDocumentRepository.save(document);
    }
}
