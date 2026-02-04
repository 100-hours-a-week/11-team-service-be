package com.thunder11.scuad.file.service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.UUID;
import java.time.Duration;


import io.awspring.cloud.s3.S3Template;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;

import com.thunder11.scuad.common.exception.ApiException;
import com.thunder11.scuad.common.exception.ErrorCode;
import com.thunder11.scuad.file.domain.FileObject;
import com.thunder11.scuad.file.repository.FileObjectRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3FileManagementService implements FileStorageService {

    private final S3Template s3Template;
    private final FileObjectRepository fileObjectRepository;

    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucket;

    @Override
    @Transactional
    public FileObject uploadFile(MultipartFile file, String directory) {
        String originalFilename = file.getOriginalFilename();
        String extension = "";

        if(originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String objectKey = directory + "/" + UUID.randomUUID() + extension;

        try (InputStream inputStream = file.getInputStream()) {
            s3Template.upload(bucket, objectKey, inputStream);
        } catch (IOException e) {
            log.error(e.getMessage());
            throw new ApiException(ErrorCode.FILE_UPLOAD_ERROR);
        }
        FileObject fileObject = FileObject.builder()
                .storageProvider("S3")
                .bucket(bucket)
                .objectKey(objectKey)
                .originalName(originalFilename)
                .contentType(file.getContentType())
                .sizeBytes(file.getSize())
                .checksum(null)
                .build();

        return fileObjectRepository.save(fileObject);
    }

    // Pre-signed URL 생성 (파일 다운로드용)
    public String generatePresignedUrl(Long fileId, Duration expiration) {
        log.info("Pre-signed URL 생성 시작: fileId={}, expiration={}", fileId, expiration);

        // 파일 조회
        FileObject fileObject = fileObjectRepository.findById(fileId)
                .orElseThrow(() -> new ApiException(ErrorCode.FILE_NOT_FOUND));

        // Soft Delete 확인
        if (fileObject.getDeletedAt() != null) {
            log.warn("삭제된 파일에 대한 URL 생성 시도: fileId={}", fileId);
            throw new ApiException(ErrorCode.FILE_NOT_FOUND);
        }

        // Pre-signed URL 생성
        try {
            URL presignedUrl = s3Template.createSignedGetURL(
                    fileObject.getBucket(),
                    fileObject.getObjectKey(),
                    expiration
            );
            log.info("Pre-signed URL 생성 완료: fileId={}", fileId);
            return presignedUrl.toString();
        } catch (Exception e) {
            log.error("Pre-signed URL 생성 실패: fileId={}, error={}", fileId, e.getMessage());
            throw new ApiException(ErrorCode.FILE_DOWNLOAD_ERROR);
        }
    }
}
