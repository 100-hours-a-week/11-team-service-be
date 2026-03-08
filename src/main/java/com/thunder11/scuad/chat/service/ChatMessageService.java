package com.thunder11.scuad.chat.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.thunder11.scuad.auth.repository.UserRepository;
import com.thunder11.scuad.chat.domain.type.MessageType;
import com.thunder11.scuad.chat.dto.request.MessageSendRequest;
import com.thunder11.scuad.file.repository.FileObjectRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.thunder11.scuad.chat.domain.ChatMessage;
import com.thunder11.scuad.chat.dto.response.ChatMessageListResponse;
import com.thunder11.scuad.chat.dto.response.ChatMessageResponse;
import com.thunder11.scuad.chat.dto.response.PaginationResponse;
import com.thunder11.scuad.chat.repository.ChatMessageRepository;
import com.thunder11.scuad.chat.repository.ChatRoomMemberRepository;
import com.thunder11.scuad.chat.repository.ChatRoomRepository;
import com.thunder11.scuad.common.exception.ApiException;
import com.thunder11.scuad.common.exception.ErrorCode;
import com.thunder11.scuad.file.service.S3FileManagementService;

// 채팅 메시지 관련 비즈니스 로직 처리
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final UserRepository userRepository;
    private final FileObjectRepository fileObjectRepository;
    private final S3FileManagementService s3FileManagementService;
    private final SimpMessagingTemplate messagingTemplate;

    private record FileInfo(Long fileId, String fileName, String contentType, Long fileSize) {
    }
    // ChatMessage -> ChatMessageResponse 변환
    private ChatMessageResponse convertToResponse(ChatMessage message, Map<Long, String> nicknameMap, Map<Long, FileInfo> fileInfoMap) {
        // 발신자 닉네임 조회
        String senderNickname;
        if (message.getSenderId() == null) {
            senderNickname = "시스템";
        } else {
            senderNickname = nicknameMap.getOrDefault(message.getSenderId(), "알 수 없음");
        }

        // 파일 정보 조회
        ChatMessageResponse.FileInfo fileInfo = null;
        if (message.getFileId() != null) {
            FileInfo info = fileInfoMap.get(message.getFileId());
            if (info != null) {
                // Pre-signed URL 생성 추가
                String fileUrl = null;
                try {
                    fileUrl = s3FileManagementService.generatePresignedUrl(
                            info.fileId,
                            java.time.Duration.ofMinutes(5)
                    );
                } catch (Exception e) {
                    log.error("Pre-signed URL 생성 실패: fileId={}, error={}", info.fileId, e.getMessage());
                    // URL 생성 실패 시에도 파일 정보는 반환 (다운로드만 안 됨)
                }

                fileInfo = ChatMessageResponse.FileInfo.builder()
                        .fileId(info.fileId)
                        .fileName(info.fileName)
                        .fileSize(info.fileSize)
                        .contentType(info.contentType)
                        .fileUrl(fileUrl)
                        .build();
            }
        }

        return ChatMessageResponse.builder()
                .messageId(message.getMessageId())
                .senderId(message.getSenderId())
                .senderNickname(senderNickname)
                .messageType(message.getMessageType())
                .content(message.getContent())
                .file(fileInfo)
                .createdAt(message.getSentAt())
                .build();
    }

    // 채팅 메시지 목록 조회 (커서 기반 페이징 + 폴링)
    public ChatMessageListResponse getMessages(
            Long chatRoomId,
            Long userId,
            Long cursor,
            int size
    ) {
        log.info("메시지 목록 조회 시작: chatRoomId={}, userId={}, cursor={}, size={}",
                chatRoomId, userId, cursor, size);

        // 1. 채팅방 존재 확인
        chatRoomRepository.findByIdNotDeleted(chatRoomId)
                .orElseThrow(() -> new ApiException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        // 2. 멤버십 확인 (참여자만 메시지 조회 가능)
        boolean isMember = chatRoomMemberRepository
                .findByChatRoomIdAndUserIdAndKickedAtIsNull(chatRoomId, userId)
                .isPresent();

        if (!isMember) {
            log.warn("채팅방 멤버가 아닌 사용자의 메시지 조회 시도: chatRoomId={}, userId={}", chatRoomId, userId);
            throw new ApiException(ErrorCode.CHAT_ROOM_ACCESS_DENIED);
        }


        // WebSocket 전환으로 since 기반 폴링 제거
        // 신규 메시지 수신은 WebSocket 구독(/topic/chat-rooms/{id})으로 처리
        // 이 API는 채팅방 입장 시 과거 메시지 로드(커서 페이징) 용도로만 사용
        List<ChatMessage> messages = chatMessageRepository.findMessagesByChatRoomIdWithCursor(
                chatRoomId,
                cursor,
                PageRequest.of(0, size + 1)
        );
        log.info("과거 메시지 조회 완료: {}개", messages.size());

        // 페이징 정보 계산
        boolean hasNext = messages.size() > size;
        if (hasNext) {
            messages = messages.subList(0, size);
        }

        Long nextCursor = null;
        if (hasNext && !messages.isEmpty()) {
            nextCursor = messages.get(messages.size() - 1).getMessageId();
        }

        PaginationResponse pagination = PaginationResponse.of(nextCursor, hasNext, messages.size());
        // 6. 발신자 닉네임 일괄 조회 (N+1 문제 해결)
        List<Long> senderIds = messages.stream()
                .map(ChatMessage::getSenderId)
                .filter(senderId -> senderId != null) // SYSTEM 메시지 제외
                .distinct()
                .collect(Collectors.toList());

        Map<Long, String> nicknameMap = new HashMap<>();
        if (!senderIds.isEmpty()) {
            List<Object[]> nicknames = userRepository.findNicknamesByUserIds(senderIds);
            nicknameMap = nicknames.stream()
                    .collect(Collectors.toMap(
                            arr -> (Long) arr[0],
                            arr -> (String) arr[1]
                    ));
        }

        // 7. 파일 정보 일괄 조회 (N+1 문제 해결) - 추가
        List<Long> fileIds = messages.stream()
                .map(ChatMessage::getFileId)
                .filter(fileId -> fileId != null) // 파일 없는 메시지 제외
                .distinct()
                .collect(Collectors.toList());

        Map<Long, FileInfo> fileInfoMap = new HashMap<>();
        if (!fileIds.isEmpty()) {
            List<Object[]> fileInfos = fileObjectRepository.findFileInfosByIds(fileIds);
            fileInfoMap = fileInfos.stream()
                    .collect(Collectors.toMap(
                            (Object[] arr) -> (Long) arr[0],
                            (Object[] arr) -> new FileInfo(
                                    (Long) arr[0],      // fileId
                                    (String) arr[1],    // fileName
                                    (String) arr[2],    // contentType
                                    (Long) arr[3]       // fileSize
                            )
                    ));
        }

        // 8. ChatMessage -> ChatMessageResponse 변환
        Map<Long, String> finalNicknameMap = nicknameMap;
        Map<Long, FileInfo> finalFileInfoMap = fileInfoMap;
        List<ChatMessageResponse> messageResponses = messages.stream()
                .map(message -> convertToResponse(message, finalNicknameMap, finalFileInfoMap))
                .collect(Collectors.toList());

        log.info("메시지 목록 조회 완료: 총 {}개", messageResponses.size());

        return ChatMessageListResponse.of(messageResponses, pagination);
    }

    // 메시지 전송
    @Transactional
    public ChatMessageResponse sendMessage(
            Long chatRoomId,
            Long userId,
            MessageSendRequest request
    ) {
        log.info("메시지 전송 시작: chatRoomId={}, userId={}, messageType={}",
                chatRoomId, userId, request.getMessageType());

        // 1. 채팅방 존재 확인
        chatRoomRepository.findByIdNotDeleted(chatRoomId)
                .orElseThrow(() -> new ApiException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        // 2. 멤버십 확인 (참여자만 메시지 전송 가능)
        boolean isMember = chatRoomMemberRepository
                .findByChatRoomIdAndUserIdAndKickedAtIsNull(chatRoomId, userId)
                .isPresent();

        if (!isMember) {
            log.warn("채팅방 멤버가 아닌 사용자의 메시지 전송 시도: chatRoomId={}, userId={}", chatRoomId, userId);
            throw new ApiException(ErrorCode.CHAT_ROOM_ACCESS_DENIED);
        }

        // 3. 메시지 타입 검증
        if (request.getMessageType() == MessageType.SYSTEM) {
            log.warn("사용자가 시스템 메시지 전송 시도: chatRoomId={}, userId={}", chatRoomId, userId);
            throw new ApiException(ErrorCode.CHAT_MESSAGE_INVALID_TYPE);
        }

        // 4. 메시지 내용 검증
        if (request.getMessageType() == MessageType.TEXT &&
                (request.getContent() == null || request.getContent().trim().isEmpty())) {
            log.warn("빈 텍스트 메시지 전송 시도: chatRoomId={}, userId={}", chatRoomId, userId);
            throw new ApiException(ErrorCode.CHAT_MESSAGE_EMPTY);
        }

        // 5. 파일 메시지 검증
        if (request.getMessageType() == MessageType.FILE && request.getFileId() == null) {
            log.warn("fileId 없는 FILE 타입 메시지: chatRoomId={}, userId={}", chatRoomId, userId);
            throw new ApiException(ErrorCode.CHAT_MESSAGE_INVALID_TYPE);
        }

        // 6. 파일 존재 여부 확인
        if (request.getMessageType() == MessageType.FILE) {
            if (!fileObjectRepository.existsByIdAndNotDeleted(request.getFileId())) {
                log.warn("존재하지 않는 파일로 메시지 전송 시도: chatRoomId={}, userId={}, fileId={}",
                        chatRoomId, userId, request.getFileId());
                throw new ApiException(ErrorCode.FILE_NOT_FOUND);
            }
        }


        // 7. 메시지 생성
        ChatMessage message = ChatMessage.builder()
                .chatRoomId(chatRoomId)
                .senderId(userId)
                .messageType(request.getMessageType())
                .content(request.getContent())
                .fileId(request.getFileId())
                .build();

        ChatMessage savedMessage = chatMessageRepository.save(message);
        log.info("메시지 전송 완료: messageId={}", savedMessage.getMessageId());

        // 8. 발신자 닉네임 조회
        String senderNickname = userRepository.findNicknameByUserId(userId)
                .orElse("알 수 없음");

        // 9. 응답 생성
        Map<Long, String> nicknameMap = new HashMap<>();
        nicknameMap.put(userId, senderNickname);

        // 10. 파일 정보 조회
        // findFileInfosByIds(List)를 재사용하여 ClassCastException 방지
        // 이유: 단건 Optional<Object[]> 쿼리는 JPQL 다중 컬럼 반환 시 Object[][] 로 감싸져
        //       arr[0]이 Long이 아닌 Object[]가 되어 ClassCastException 발생.
        //       getMessages()에서 이미 검증된 List<Object[]> 방식으로 통일.
        Map<Long, FileInfo> fileInfoMap = new HashMap<>();
        if (savedMessage.getFileId() != null) {
            List<Object[]> fileInfos = fileObjectRepository.findFileInfosByIds(
                    java.util.List.of(savedMessage.getFileId())
            );
            if (!fileInfos.isEmpty()) {
                Object[] arr = fileInfos.get(0);
                FileInfo info = new FileInfo(
                        (Long) arr[0],      // fileId
                        (String) arr[1],    // fileName
                        (String) arr[2],    // contentType
                        (Long) arr[3]       // fileSize
                );
                fileInfoMap.put(info.fileId(), info);
            }
        }

        ChatMessageResponse result = convertToResponse(savedMessage, nicknameMap, fileInfoMap);

        // WebSocket 브로드캐스트
        // 해당 채팅방을 구독 중인 모든 클라이언트에게 새 메시지를 즉시 push
        // 클라이언트는 /topic/chat-rooms/{chatRoomId} 를 구독하고 있어야 수신 가능
        messagingTemplate.convertAndSend("/topic/chat-rooms/" + chatRoomId, result);
        log.info("WebSocket 브로드캐스트 완료: chatRoomId={}, messageId={}", chatRoomId, savedMessage.getMessageId());

        return result;
    }
}