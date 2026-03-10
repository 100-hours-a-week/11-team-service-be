package com.thunder11.scuad.file.dto.response;

import com.thunder11.scuad.file.domain.FileObject;
import lombok.Builder;
import lombok.Getter;

// 파일 업로드 응답 DTO
// POST /api/v1/files/upload 응답용
// fileId: 이후 PATCH /api/v1/users/me 등에서 참조할 파일 식별자
// downloadUrl: 업로드 직후 클라이언트가 이미지를 즉시 미리보기할 수 있도록 presigned URL 포함
@Getter
@Builder
public class FileUploadResponse {

    private Long fileId;
    private String originalName;
    private String contentType;
    private Long sizeBytes;
    private String downloadUrl;     // 업로드 직후 미리보기용 presigned URL (10분 유효)

    // FileObject 엔티티와 presigned URL로 응답 DTO 생성
    public static FileUploadResponse of(FileObject fileObject, String downloadUrl) {
        return FileUploadResponse.builder()
                .fileId(fileObject.getId())
                .originalName(fileObject.getOriginalName())
                .contentType(fileObject.getContentType())
                .sizeBytes(fileObject.getSizeBytes())
                .downloadUrl(downloadUrl)
                .build();
    }
}
