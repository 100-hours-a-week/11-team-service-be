package com.thunder11.scuad.user.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.thunder11.scuad.auth.domain.User;
import com.thunder11.scuad.auth.repository.UserOAuthAccountRepository;
import com.thunder11.scuad.auth.repository.UserRepository;
import com.thunder11.scuad.common.exception.ApiException;
import com.thunder11.scuad.common.exception.ErrorCode;
import com.thunder11.scuad.user.dto.UserResponse;

import lombok.RequiredArgsConstructor;

/**
 * User 도메인 서비스
 * 사용자 정보 조회 및 관리
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final UserOAuthAccountRepository userOAuthAccountRepository;

    /**
     * 현재 로그인한 사용자 정보 조회
     * 
     * @param userId JWT에서 추출한 사용자 ID
     * @return UserResponse 사용자 정보
     * @throws ApiException USER_NOT_FOUND 사용자를 찾을 수 없는 경우
     */
    public UserResponse getCurrentUser(Long userId) {
        // 1. users 테이블에서 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        // 2. user_oauth_accounts 테이블에서 이메일 조회 (KAKAO 기준)
        String email = userOAuthAccountRepository.findEmailByUserId(userId)
                .orElse(null);  // 이메일이 없을 수 있음 (카카오 동의 안 함)

        // 3. UserResponse 생성 및 반환
        return UserResponse.of(user, email);
    }
}
