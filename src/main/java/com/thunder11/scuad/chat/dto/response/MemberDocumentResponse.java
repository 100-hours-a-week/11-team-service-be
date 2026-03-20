package com.thunder11.scuad.chat.dto.response;

import lombok.Builder;
import lombok.Getter;

// 채팅방 멤버 문서(이력서/포트폴리오) 조회 응답 DTO
@Getter
@Builder
public class MemberDocumentResponse {

    private boolean hasDocument;   // 문서 제출 여부 (포트폴리오는 선택 제출이므로 false 가능)
    private Long fileId;
    private String fileName;       // 원본 파일명
    private String contentType;    // MIME 타입 (application/pdf 등)
    private Long fileSize;         // 파일 크기 (bytes)
    private String fileUrl;        // Pre-signed URL (15분 만료)

    // 문서가 존재하는 경우
    public static MemberDocumentResponse of(
            Long fileId,
            String fileName,
            String contentType,
            Long fileSize,
            String fileUrl
    ) {
        return MemberDocumentResponse.builder()
                .hasDocument(true)
                .fileId(fileId)
                .fileName(fileName)
                .contentType(contentType)
                .fileSize(fileSize)
                .fileUrl(fileUrl)
                .build();
    }

    // 문서가 없는 경우 (포트폴리오 미제출)
    // 추가 근거: 포트폴리오는 선택 제출이므로 없을 때 404 대신 200 + hasDocument=false 반환
    //           프론트에서 hasDocument 값으로 "제출된 포트폴리오가 없습니다" 분기 처리 가능
    public static MemberDocumentResponse empty() {
        return MemberDocumentResponse.builder()
                .hasDocument(false)
                .build();
    }
}