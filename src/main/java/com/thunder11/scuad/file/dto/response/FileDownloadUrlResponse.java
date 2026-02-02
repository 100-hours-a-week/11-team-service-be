package com.thunder11.scuad.file.dto.response;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.thunder11.scuad.file.controller.FileController;
import lombok.Builder;
import lombok.Getter;

// 파일 다운로드 URL 응답
@Getter
@Builder
public class FileDownloadUrlResponse {

    private String downloadUrl;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expiresAt;

    public static FileDownloadUrlResponse of(String downloadUrl) {
        return FileDownloadUrlResponse.builder()
                .downloadUrl(downloadUrl)
                .expiresAt(LocalDateTime.now().plusMinutes(10)) // 10분 후 만료
                .build();
    }
}