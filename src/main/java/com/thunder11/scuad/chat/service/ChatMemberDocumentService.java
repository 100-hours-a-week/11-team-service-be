package com.thunder11.scuad.chat.service;

import java.time.Duration;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.thunder11.scuad.chat.domain.ChatRoomMember;
import com.thunder11.scuad.chat.dto.response.MemberDocumentResponse;
import com.thunder11.scuad.chat.repository.ChatRoomMemberRepository;
import com.thunder11.scuad.chat.repository.ChatRoomRepository;
import com.thunder11.scuad.common.exception.ApiException;
import com.thunder11.scuad.common.exception.ErrorCode;
import com.thunder11.scuad.file.service.S3FileManagementService;
import com.thunder11.scuad.jobposting.domain.ApplicationDocument;
import com.thunder11.scuad.jobposting.domain.type.ApplicationDocumentType;
import com.thunder11.scuad.jobposting.repository.ApplicationDocumentRepository;

// 채팅방 멤버 문서(이력서/포트폴리오) 조회 서비스

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatMemberDocumentService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ApplicationDocumentRepository applicationDocumentRepository;
    private final S3FileManagementService s3FileManagementService;

    // Pre-signed URL 만료 시간: 15분
    private static final Duration DOCUMENT_URL_EXPIRATION = Duration.ofMinutes(15);

    // 채팅방 멤버 이력서 조회
    public MemberDocumentResponse getMemberResume(Long chatRoomId, Long requestUserId, Long chatRoomMemberId) {
        log.info("멤버 이력서 조회: chatRoomId={}, requestUserId={}, targetMemberId={}",
                chatRoomId, requestUserId, chatRoomMemberId);

        return getMemberDocument(chatRoomId, requestUserId, chatRoomMemberId, ApplicationDocumentType.RESUME);
    }

    // 채팅방 멤버 포트폴리오 조회
    // 수정 근거: 포트폴리오는 선택 제출이므로 없을 때 404(DOCUMENT_NOT_FOUND)를 던지는 것은 부적절.
    //           미제출 상태도 정상적인 케이스이므로 getMemberDocument 공통 로직을 거치지 않고
    //           별도로 처리하여 200 + hasDocument=false 응답을 반환
    public MemberDocumentResponse getMemberPortfolio(Long chatRoomId, Long requestUserId, Long chatRoomMemberId) {
        log.info("멤버 포트폴리오 조회: chatRoomId={}, requestUserId={}, targetMemberId={}",
                chatRoomId, requestUserId, chatRoomMemberId);

        // 1. 채팅방 존재 확인
        chatRoomRepository.findByIdNotDeleted(chatRoomId)
                .orElseThrow(() -> new ApiException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        // 2. 요청자 권한 확인
        chatRoomMemberRepository
                .findByChatRoomIdAndUserIdAndKickedAtIsNull(chatRoomId, requestUserId)
                .orElseThrow(() -> new ApiException(ErrorCode.CHAT_ROOM_ACCESS_DENIED));

        // 3. 대상 멤버 조회
        ChatRoomMember targetMember = chatRoomMemberRepository
                .findByChatRoomMemberIdAndKickedAtIsNull(chatRoomMemberId)
                .orElseThrow(() -> new ApiException(ErrorCode.CHAT_MEMBER_NOT_FOUND));

        // 4. chatRoomId 교차 검증
        if (!targetMember.getChatRoomId().equals(chatRoomId)) {
            throw new ApiException(ErrorCode.CHAT_MEMBER_NOT_FOUND);
        }

        // 5. 포트폴리오 조회 — 없으면 예외 대신 empty() 반환 (선택 제출이므로 404 부적절)
        return applicationDocumentRepository
                .findByJobApplication_IdAndDocType(
                        targetMember.getJobApplicationId(),
                        ApplicationDocumentType.PORTFOLIO
                )
                .map(document -> {
                    String fileUrl = s3FileManagementService.generatePresignedUrl(
                            document.getFile().getId(),
                            DOCUMENT_URL_EXPIRATION
                    );
                    return MemberDocumentResponse.of(
                            document.getFile().getId(),
                            document.getFile().getOriginalName(),
                            document.getFile().getContentType(),
                            document.getFile().getSizeBytes(),
                            fileUrl
                    );
                })
                .orElseGet(() -> {
                    log.info("포트폴리오 미제출: chatRoomMemberId={}", chatRoomMemberId);
                    return MemberDocumentResponse.empty();
                });
    }

    // 공통 문서 조회 로직
    private MemberDocumentResponse getMemberDocument(
            Long chatRoomId,
            Long requestUserId,
            Long chatRoomMemberId,
            ApplicationDocumentType docType
    ) {
        // 1. 채팅방 존재 확인
        chatRoomRepository.findByIdNotDeleted(chatRoomId)
                .orElseThrow(() -> new ApiException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        // 2. 요청자가 채팅방 멤버인지 확인 (방 멤버만 타인의 문서 열람 가능)
        chatRoomMemberRepository
                .findByChatRoomIdAndUserIdAndKickedAtIsNull(chatRoomId, requestUserId)
                .orElseThrow(() -> new ApiException(ErrorCode.CHAT_ROOM_ACCESS_DENIED));

        // 3. 대상 멤버 조회
        ChatRoomMember targetMember = chatRoomMemberRepository
                .findByChatRoomMemberIdAndKickedAtIsNull(chatRoomMemberId)
                .orElseThrow(() -> new ApiException(ErrorCode.CHAT_MEMBER_NOT_FOUND));

        // 4. 대상 멤버가 해당 채팅방 소속인지 검증
        //    (다른 채팅방 멤버 ID로 요청하는 경우 방어)
        if (!targetMember.getChatRoomId().equals(chatRoomId)) {
            throw new ApiException(ErrorCode.CHAT_MEMBER_NOT_FOUND);
        }

        // 5. 문서 조회 (포트폴리오는 선택 제출이므로 없으면 DOCUMENT_NOT_FOUND → 프론트 토스트 처리)
        ApplicationDocument document = applicationDocumentRepository
                .findByJobApplication_IdAndDocType(
                        targetMember.getJobApplicationId(),
                        docType
                )
                .orElseThrow(() -> new ApiException(ErrorCode.DOCUMENT_NOT_FOUND));

        // 6. Pre-signed URL 생성 (15분 만료)
        String fileUrl = s3FileManagementService.generatePresignedUrl(
                document.getFile().getId(),
                DOCUMENT_URL_EXPIRATION
        );

        return MemberDocumentResponse.of(
                document.getFile().getId(),
                document.getFile().getOriginalName(),
                document.getFile().getContentType(),
                document.getFile().getSizeBytes(),
                fileUrl
        );
    }
}