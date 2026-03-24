package com.thunder11.scuad.jobposting.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.thunder11.scuad.auth.domain.User;
import com.thunder11.scuad.auth.repository.UserRepository;
import com.thunder11.scuad.common.exception.ApiException;
import com.thunder11.scuad.common.exception.ErrorCode;
import com.thunder11.scuad.file.domain.FileObject;
import com.thunder11.scuad.file.service.FileStorageService;
import com.thunder11.scuad.jobposting.domain.AiApplicantEvaluation;
import com.thunder11.scuad.jobposting.domain.ApplicationDocument;
import com.thunder11.scuad.jobposting.domain.JobApplication;
import com.thunder11.scuad.jobposting.domain.JobMaster;
import com.thunder11.scuad.jobposting.domain.type.ApplicationDocumentType;
import com.thunder11.scuad.jobposting.domain.type.ApplicationStatus;
import com.thunder11.scuad.jobposting.dto.response.MyApplicationResponse;
import com.thunder11.scuad.jobposting.repository.AiApplicationEvaluationRepository;
import com.thunder11.scuad.jobposting.repository.AiPortfolioAnalysisRepository;
import com.thunder11.scuad.jobposting.repository.AiResumeAnalysisRepository;
import com.thunder11.scuad.jobposting.repository.ApplicationDocumentRepository;
import com.thunder11.scuad.jobposting.repository.JobApplicationRepository;
import com.thunder11.scuad.jobposting.repository.JobMasterRepository;
import com.thunder11.scuad.jobposting.dto.response.JobApplicationDetailResponse;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobApplicationService {

    private final UserRepository userRepository;
    private final JobMasterRepository jobMasterRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final ApplicationDocumentRepository applicationDocumentRepository;
    private final FileStorageService fileStorageService;
    private final JopApplicationAnalysisService analysisService;
    private final AiApplicationEvaluationRepository aiApplicationEvaluationRepository;
    private final AiResumeAnalysisRepository aiResumeAnalysisRepository;
    private final AiPortfolioAnalysisRepository aiPortfolioAnalysisRepository;

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
        analysisService.createEvaluationJob(application.getId(), userId, "RESUME");

        if (portfolio != null && !portfolio.isEmpty()) {
            analysisService.createEvaluationJob(application.getId(), userId, "PORTFOLIO");
        }

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

        Optional<ApplicationDocument> existingDoc = applicationDocumentRepository
                .findByJobApplication_IdAndDocType(applicationId, type);

        if (existingDoc.isPresent()) {
            return updateDocument(existingDoc.get(), file, docType);
        } else {
            return saveDocument(jobApplication, docType, file);
        }
    }

    @Transactional(readOnly = true)
    public List<MyApplicationResponse> getMyApplications(Long userId, String keyword) {
        List<JobApplication> applications = jobApplicationRepository.findMyApplication(userId, keyword);
        if (applications.isEmpty()) {
            return List.of();
        }

        List<Long> applicationIds = applications.stream().map(JobApplication::getId).toList();

        Map<Long, Integer> scoreMap = aiApplicationEvaluationRepository.findAllByJobApplicationIdIn(applicationIds)
                .stream()
                .collect(Collectors.toMap(e -> e.getJobApplication().getId(), AiApplicantEvaluation::getOverallScore,
                        (a, b) -> a));

        List<Long> resumeAnalyzedIds = aiResumeAnalysisRepository.findAllByJobApplicationIdIn(applicationIds)
                .stream().map(e -> e.getJobApplication().getId()).toList();
        List<Long> portfolioAnalyzedIds = aiPortfolioAnalysisRepository.findAllByJobApplicationIdIn(applicationIds)
                .stream().map(e -> e.getJobApplication().getId()).toList();

        return applications.stream()
                .map(ja -> {
                    Integer score = scoreMap.get(ja.getId());
                    boolean resumeAnalyzed = resumeAnalyzedIds.contains(ja.getId());
                    boolean portfolioAnalyzed = portfolioAnalyzedIds.contains(ja.getId());

                    boolean resumeRegistered = ja.getApplicationDocuments().stream()
                            .anyMatch(d -> d.getDocType() == ApplicationDocumentType.RESUME);
                    boolean portfolioRegistered = ja.getApplicationDocuments().stream()
                            .anyMatch(d -> d.getDocType() == ApplicationDocumentType.PORTFOLIO);

                    boolean isProcessing = analysisService.isProcessing(ja.getId());
                    return MyApplicationResponse.from(ja, score, isProcessing, resumeAnalyzed, portfolioAnalyzed,
                            resumeRegistered, portfolioRegistered);
                })
                .toList();
    }

    private void validateFile(MultipartFile file, String docType) {

        if (!"application/pdf".equals(file.getContentType())) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "PDF 파일만 업로드 가능합니다.");
        }
    }

    @Transactional(readOnly = true)
    public JobApplicationDetailResponse getJobApplicationDetail(Long userId, Long applicationId) {
        JobApplication application = jobApplicationRepository.findByIdAndUserUserId(applicationId, userId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "지원 내역을 찾을 수 없거나 접근 권한이 없습니다."));

        List<ApplicationDocument> documents = applicationDocumentRepository
                .findAllByJobApplication_Id(application.getId());

        log.info("지원서 조회 - ID: {}, 찾은 서류 개수: {}", application.getId(), documents.size());
        documents.forEach(doc -> log.info("  - 서류: ID={}, Type={}, DeletedAt={}", doc.getId(), doc.getDocType(),
                doc.getDeletedAt()));

        Map<ApplicationDocumentType, ApplicationDocument> docMap = documents.stream()
                .collect(Collectors.toMap(ApplicationDocument::getDocType, doc -> doc));

        List<JobApplicationDetailResponse.ApplicationDocumentResponse> documentResponses = List.of(
                createDocumentResponse(docMap.get(ApplicationDocumentType.RESUME), "RESUME"),
                createDocumentResponse(docMap.get(ApplicationDocumentType.PORTFOLIO), "PORTFOLIO"));

        return JobApplicationDetailResponse.of(application, documentResponses);
    }

    private JobApplicationDetailResponse.ApplicationDocumentResponse createDocumentResponse(ApplicationDocument doc,
            String typeStr) {
        if (doc == null) {
            return JobApplicationDetailResponse.ApplicationDocumentResponse.builder()
                    .docType(typeStr)
                    .isRegistered(false)
                    .build();
        }
        return JobApplicationDetailResponse.ApplicationDocumentResponse.builder()
                .docType(typeStr)
                .isRegistered(true)
                .originalFileName(doc.getFile().getOriginalName())
                .fileUrl(String.valueOf(doc.getFile().getId()))
                .build();
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

    private ApplicationDocument updateDocument(ApplicationDocument existingDoc, MultipartFile file, String docType) {
        FileObject oldFile = existingDoc.getFile();
        fileStorageService.deleteFile(oldFile.getId());

        String uploadPath = "applications/" + existingDoc.getId() + "/" + docType.toLowerCase();
        FileObject newFile = fileStorageService.uploadFile(file, uploadPath);

        existingDoc.updateFile(newFile);

        return applicationDocumentRepository.save(existingDoc);
    }
}
