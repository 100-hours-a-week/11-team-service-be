package com.thunder11.scuad.jobposting.controller;

import com.thunder11.scuad.common.response.ApiResponse;
import com.thunder11.scuad.jobposting.domain.ApplicationDocument;
import com.thunder11.scuad.jobposting.dto.response.DocumentResponse;
import com.thunder11.scuad.jobposting.service.JobApplicationService;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/applications")
@RequiredArgsConstructor
public class JobApplicationController {

    private final JobApplicationService jobApplicationService;

    @PostMapping("/{applicationId}/documents")
    public ApiResponse<DocumentResponse> uploadDocument(
            @PathVariable Long applicationId,
            @RequestParam("doc_type") String docType,
            @RequestParam("file") MultipartFile file
    ) {
        ApplicationDocument savedDoc = jobApplicationService.uploadDocument(applicationId, docType, file);
        DocumentResponse result = DocumentResponse.from(savedDoc);

        return ApiResponse.of(200, "DOCUMENT_UPLOAD_SUCCESS", "서류 업로드에 성공했습니다.", result);
    }
}
