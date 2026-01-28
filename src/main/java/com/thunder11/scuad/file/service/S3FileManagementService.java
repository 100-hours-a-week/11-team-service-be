package com.thunder11.scuad.file.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

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
import com.thunder11.scuad.file.repository.FileObjcetRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3FileManagementService implements FileStorageService {

    private final S3Template s3Template;
    private final FileObjcetRepository fileObjcetRepository;

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

        return fileObjcetRepository.save(fileObject);
    }
}
