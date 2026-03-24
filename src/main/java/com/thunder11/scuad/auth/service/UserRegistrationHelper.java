package com.thunder11.scuad.auth.service;

import com.thunder11.scuad.auth.domain.*;
import com.thunder11.scuad.auth.repository.UserOAuthAccountRepository;
import com.thunder11.scuad.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

// 신규 유저 저장 전용 헬퍼 빈
//
// 분리 이유:
//   AuthService.processKakaoCallback()은 @Transactional이므로,
//   내부에서 saveAndFlush()가 DataIntegrityViolationException을 던지면
//   JPA 세션이 오염(rollback-only)되어 재시도 시 AssertionFailure 발생.
//
//   이 헬퍼를 REQUIRES_NEW로 선언하면 호출마다 독립 트랜잭션이 생성되고,
//   예외 발생 시 해당 트랜잭션만 롤백 → 외부 세션은 오염되지 않음.
//   → 재시도마다 깨끗한 세션으로 시도 가능
@Slf4j
@Component
@RequiredArgsConstructor
public class UserRegistrationHelper {

    private final UserRepository userRepository;
    private final UserOAuthAccountRepository oAuthAccountRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public UserOAuthAccount tryCreateUser(
            String nickname,
            String email,
            String providerUserId,
            OAuthProvider provider
    ) {
        User newUser = User.builder()
                .nickname(nickname)
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .build();

        User savedUser = userRepository.saveAndFlush(newUser);
        log.info("신규 사용자 생성: userId={}, nickname={}", savedUser.getUserId(), nickname);

        UserOAuthAccount oAuthAccount = UserOAuthAccount.builder()
                .user(savedUser)
                .email(email)
                .provider(provider)
                .providerUserId(providerUserId)
                .providerEmail(email)
                .connectedAt(LocalDateTime.now())
                .build();

        UserOAuthAccount savedOAuthAccount = oAuthAccountRepository.saveAndFlush(oAuthAccount);
        log.info("OAuth 계정 연동 완료: provider={}, providerUserId={}", provider, providerUserId);

        return savedOAuthAccount;
    }
}