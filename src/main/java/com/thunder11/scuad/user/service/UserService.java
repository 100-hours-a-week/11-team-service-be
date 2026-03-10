package com.thunder11.scuad.user.service;

import java.time.Duration;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.thunder11.scuad.auth.domain.User;
import com.thunder11.scuad.auth.domain.UserStatus;
import com.thunder11.scuad.auth.repository.UserOAuthAccountRepository;
import com.thunder11.scuad.auth.repository.UserRepository;
import com.thunder11.scuad.common.exception.ApiException;
import com.thunder11.scuad.common.exception.ErrorCode;
import com.thunder11.scuad.file.service.S3FileManagementService;
import com.thunder11.scuad.user.dto.UserResponse;
import org.springframework.dao.DataIntegrityViolationException;
import com.thunder11.scuad.file.repository.FileObjectRepository;
import com.thunder11.scuad.user.dto.request.UserUpdateRequest;

import lombok.RequiredArgsConstructor;

// User 도메인 서비스
// 사용자 정보 조회 및 관리
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final UserOAuthAccountRepository userOAuthAccountRepository;
    private final S3FileManagementService s3FileManagementService;
    private final FileObjectRepository fileObjectRepository;
    // presigned URL 유효 시간: 클라이언트 세션 내 이미지 표시에 충분한 시간으로 설정
    private static final Duration PROFILE_IMAGE_URL_EXPIRATION = Duration.ofMinutes(10);

    // 현재 로그인한 사용자 정보 조회
    // WITHDRAWN 상태 사용자는 403 반환 (탈퇴 완료 후 잔여 토큰 재사용 차단)
    public UserResponse getCurrentUser(Long userId) {
        // 1. 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        // 2. 탈퇴 사용자 접근 차단
        if (user.getStatus() == UserStatus.WITHDRAWN) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }

        // 3. user_oauth_accounts에서 이메일 조회 (KAKAO 기준, null 허용)
        String email = userOAuthAccountRepository.findEmailByUserId(userId)
                .orElse(null);

        // 4. 프로필 이미지 presigned URL 생성 (이미지가 없으면 null 반환)
        String profileImageUrl = resolveProfileImageUrl(user.getProfileImageFileId());

        return UserResponse.of(user, email, profileImageUrl);
    }

    // 회원정보 수정
    // 닉네임 중복 시 DataIntegrityViolationException을 NICKNAME_DUPLICATE로 변환
    // DB 예외가 컨트롤러 계층에 노출되면 클라이언트가 원인을 알 수 없으므로 도메인 예외로 변환
    @Transactional
    public UserResponse updateUser(Long userId, UserUpdateRequest request) {
        // 1. 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        // 2. 탈퇴 사용자 수정 차단
        if (user.getStatus() == UserStatus.WITHDRAWN) {
            throw new ApiException(ErrorCode.USER_ALREADY_WITHDRAWN);
        }

        // 3. 닉네임 수정 (요청 시)
        if (request.hasNicknameUpdate()) {
            // 공백만으로 구성된 닉네임 차단 ("   " 등 실질적 빈 값)
            if (request.getNickname().isBlank()) {
                throw new ApiException(ErrorCode.VALIDATION_FAILED);
            }
            user.updateNickname(request.getNickname());
        }

        // 4. 프로필 이미지 수정 (요청 시)
        // 존재하지 않는 파일 ID 저장 시 FK 위반 및 데이터 불일치 방지를 위해 사전 검증
        if (request.hasProfileImageUpdate()) {
            boolean fileExists = fileObjectRepository.existsByIdAndNotDeleted(
                    request.getProfileImageFileId()
            );
            if (!fileExists) {
                throw new ApiException(ErrorCode.FILE_NOT_FOUND);
            }
            user.updateProfileImageFileId(request.getProfileImageFileId());
        }

        // 5. 저장 (닉네임 중복 시 DataIntegrityViolationException → NICKNAME_DUPLICATE 변환)
        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            throw new ApiException(ErrorCode.NICKNAME_DUPLICATE);
        }

        // 6. 변경된 사용자 정보 반환
        String email = userOAuthAccountRepository.findEmailByUserId(userId).orElse(null);
        String profileImageUrl = resolveProfileImageUrl(user.getProfileImageFileId());
        return UserResponse.of(user, email, profileImageUrl);
    }

    // 프로필 이미지 presigned URL 생성 헬퍼
    // fileId가 null이거나 S3 오류 발생 시 null 반환
    // 이미지 오류가 사용자 정보 전체 조회를 실패시키지 않도록 예외를 흡수
    private String resolveProfileImageUrl(Long profileImageFileId) {
        if (profileImageFileId == null) {
            return null;
        }
        try {
            return s3FileManagementService.generatePresignedUrl(
                    profileImageFileId,
                    PROFILE_IMAGE_URL_EXPIRATION
            );
        } catch (ApiException e) {
            return null;
        }
    }
}