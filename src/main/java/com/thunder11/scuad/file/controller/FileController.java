package com.thunder11.scuad.file.controller;

import java.time.Duration;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.thunder11.scuad.auth.security.UserPrincipal;
import com.thunder11.scuad.common.exception.ApiException;
import com.thunder11.scuad.common.exception.ErrorCode;
import com.thunder11.scuad.common.response.ApiResponse;
import com.thunder11.scuad.file.domain.FileObject;
import com.thunder11.scuad.file.dto.response.FileDownloadUrlResponse;
import com.thunder11.scuad.file.dto.response.FileUploadResponse;
import com.thunder11.scuad.file.service.S3FileManagementService;

// 파일 관련 API 컨트롤러
@Slf4j
@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileController {

    private final S3FileManagementService fileManagementService;

    // 기본 업로드 디렉토리: 경로 미지정 시 사용
    private static final String DEFAULT_DIRECTORY = "general";

    // 파일 업로드
    // POST /api/v1/files/upload
    // 이미지 타입(image/*)만 허용: 프로필 이미지 등 이미지 전용 업로드 용도
    // directory 파라미터로 S3 경로 분리 가능 (예: profiles, resumes)
    // 업로드 완료 후 fileId와 미리보기용 presigned URL을 함께 반환
    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<FileUploadResponse>> uploadFile(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "directory", defaultValue = DEFAULT_DIRECTORY) String directory
    ) {
        log.info("POST /api/v1/files/upload: userId={}, directory={}, contentType={}",
                userPrincipal.getUserId(), directory, file.getContentType());

        // 이미지 타입 검증: null이거나 image/로 시작하지 않으면 거부
        // S3 업로드 전에 차단하여 불필요한 네트워크 비용 방지
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new ApiException(ErrorCode.INVALID_FILE_TYPE);
        }

        // 빈 파일 업로드 차단
        if (file.isEmpty()) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED);
        }

        FileObject fileObject = fileManagementService.uploadFile(file, directory);

        // 업로드 직후 미리보기용 presigned URL 생성 (10분 유효)
        String downloadUrl = fileManagementService.generatePresignedUrl(
                fileObject.getId(),
                Duration.ofMinutes(10)
        );

        FileUploadResponse response = FileUploadResponse.of(fileObject, downloadUrl);

        return ResponseEntity.ok(
                ApiResponse.of(200, "FILE_002", "파일이 성공적으로 업로드되었습니다.", response)
        );
    }

    // 파일 다운로드 URL 생성 (10분 유효)
    @GetMapping("/{fileId}/download-url")
    public ApiResponse<FileDownloadUrlResponse> getDownloadUrl(@PathVariable Long fileId) {
        log.info("GET /api/v1/files/{}/download-url", fileId);

        String downloadUrl = fileManagementService.generatePresignedUrl(fileId, Duration.ofMinutes(10));
        FileDownloadUrlResponse response = FileDownloadUrlResponse.of(downloadUrl);

        return ApiResponse.of(200, "FILE_001", "파일 다운로드 URL이 생성되었습니다.", response);
    }
}