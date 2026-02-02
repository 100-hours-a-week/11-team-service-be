package com.thunder11.scuad.file.controller;

import java.time.Duration;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.thunder11.scuad.common.response.ApiResponse;
import com.thunder11.scuad.file.dto.response.FileDownloadUrlResponse;
import com.thunder11.scuad.file.service.S3FileManagementService;

// 파일 관련 API 컨트롤러
@Slf4j
@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileController {

    private final S3FileManagementService fileManagementService;

    // 파일 다운로드 URL 생성 (10분 유효)
    @GetMapping("/{fileId}/download-url")
    public ApiResponse<FileDownloadUrlResponse> getDownloadUrl(@PathVariable Long fileId) {
        log.info("GET /api/v1/files/{}/download-url", fileId);

        String downloadUrl = fileManagementService.generatePresignedUrl(fileId, Duration.ofMinutes(10));
        FileDownloadUrlResponse response = FileDownloadUrlResponse.of(downloadUrl);

        return ApiResponse.of(200, "FILE_001", "파일 다운로드 URL이 생성되었습니다.", response);
    }
}