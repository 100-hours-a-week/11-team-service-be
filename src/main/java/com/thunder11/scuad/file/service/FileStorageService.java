package com.thunder11.scuad.file.service;

import org.springframework.web.multipart.MultipartFile;

import com.thunder11.scuad.file.domain.FileObject;

public interface FileStorageService {
    FileObject uploadFile(MultipartFile file, String directory);
}