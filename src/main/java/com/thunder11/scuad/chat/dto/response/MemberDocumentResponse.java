package com.thunder11.scuad.chat.dto.response;

import lombok.Builder;
import lombok.Getter;

// 채팅방 멤버 문서(이력서/포트폴리오) 조회 응답 DTO
@Getter
@Builder
public class MemberDocumentResponse {

    private Long fileId;
    private String fileName;       // 원본 파일명
    private String contentType;    // MIME 타입 (application/pdf 등)
    private Long fileSize;         // 파일 크기 (bytes)
    private String fileUrl;        // Pre-signed URL (15분 만료)

    public static MemberDocumentResponse of(
            Long fileId,
            String fileName,
            String contentType,
            Long fileSize,
            String fileUrl
    ) {
        return MemberDocumentResponse.builder()
                .fileId(fileId)
                .fileName(fileName)
                .contentType(contentType)
                .fileSize(fileSize)
                .fileUrl(fileUrl)
                .build();
    }
}