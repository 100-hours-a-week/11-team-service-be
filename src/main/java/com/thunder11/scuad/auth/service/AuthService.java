package com.thunder11.scuad.auth.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.thunder11.scuad.auth.domain.*;
import com.thunder11.scuad.auth.dto.TokenRefreshResponse;
import com.thunder11.scuad.common.exception.ApiException;
import com.thunder11.scuad.common.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import com.thunder11.scuad.auth.client.KakaoOAuthClient;
import com.thunder11.scuad.auth.config.JwtProperties;
import com.thunder11.scuad.auth.config.KakaoProperties;
import com.thunder11.scuad.auth.dto.KakaoTokenResponse;
import com.thunder11.scuad.auth.dto.KakaoUserInfoResponse;
import com.thunder11.scuad.auth.dto.LoginResponse;
import com.thunder11.scuad.auth.repository.AuthRefreshTokenRepository;
import com.thunder11.scuad.auth.repository.UserOAuthAccountRepository;
import com.thunder11.scuad.auth.repository.UserRepository;
import com.thunder11.scuad.auth.util.JwtProvider;

import com.thunder11.scuad.auth.util.TokenHasher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// 인증/인가 비즈니스 로직 처리
// 카카오 OAuth 로그인, JWT 발급, 토큰 재발급 등
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final KakaoProperties kakaoProperties;
    private final KakaoOAuthClient kakaoOAuthClient;
    private final JwtProvider jwtProvider;
    private final JwtProperties jwtProperties;
    
    private final UserRepository userRepository;
    private final UserOAuthAccountRepository oAuthAccountRepository;
    private final AuthRefreshTokenRepository refreshTokenRepository;

    // 카카오 OAuth 인증 URL 생성
    // 프론트엔드를 카카오 로그인 페이지로 리다이렉트하기 위한 URL 생성
    public String getKakaoAuthUrl(String state) {
        // UriComponentsBuilder로 쿼리 파라미터 안전하게 조합
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString("https://kauth.kakao.com/oauth/authorize")
                .queryParam("client_id", kakaoProperties.getClientId())
                .queryParam("redirect_uri", kakaoProperties.getRedirectUri())
                .queryParam("response_type", "code");

        // state가 있으면 추가 (CSRF 방지용)
        if (state != null && !state.isEmpty()) {
            builder.queryParam("state", state);
        }

        String authUrl = builder.toUriString();
        log.info("카카오 OAuth 인증 URL 생성: redirectUri={}", kakaoProperties.getRedirectUri());

        return authUrl;
    }

    // 카카오 로그인 콜백 처리
    // 인가 코드로 토큰 발급 → 사용자 정보 조회 → 회원가입/로그인 → JWT 발급
    @Transactional
    public LoginResponse processKakaoCallback(String code) {
        // 1. 카카오 액세스 토큰 발급
        KakaoTokenResponse tokenResponse = kakaoOAuthClient.getAccessToken(code);
        log.info("카카오 액세스 토큰 발급 완료");

        // 2. 카카오 사용자 정보 조회
        KakaoUserInfoResponse userInfo = kakaoOAuthClient.getUserInfo(tokenResponse.getAccessToken());
        String kakaoUserId = String.valueOf(userInfo.getId());
        log.info("카카오 사용자 정보 조회 완료: kakaoUserId={}", kakaoUserId);

        // 3. 기존 사용자 조회 또는 신규 회원가입
        UserOAuthAccount oAuthAccount = oAuthAccountRepository
                .findByProviderAndProviderUserId(OAuthProvider.KAKAO, kakaoUserId)
                .orElseGet(() -> registerNewUser(userInfo));

        User user = oAuthAccount.getUser();
        log.info("사용자 인증 완료: userId={}, nickname={}", user.getUserId(), user.getNickname());

        // 4. JWT 토큰 발급
        String accessToken = jwtProvider.generateAccessToken(user.getUserId(), user.getRole().name());
        String refreshToken = jwtProvider.generateRefreshToken();

        // 5. Refresh Token DB 저장
        saveRefreshToken(user, refreshToken);

        // 6. 응답 생성
        return LoginResponse.of(
                accessToken,
                refreshToken,
                jwtProperties.getAccessTokenExpiration() / 1000  // 밀리초 → 초 변환
        );
    }

    // 신규 사용자 회원가입 처리
    // 카카오 정보로 User와 UserOAuthAccount 생성
    private UserOAuthAccount registerNewUser(KakaoUserInfoResponse userInfo) {
        // 1. User 엔티티 생성
        String nickname = generateUniqueNickname(userInfo);
        
        User newUser = User.builder()
                .nickname(nickname)
                .role(Role.USER)  // 기본 역할
                .status(UserStatus.ACTIVE)  // 활성 상태
                .build();
        
        User savedUser = userRepository.save(newUser);
        log.info("신규 사용자 생성: userId={}, nickname={}", savedUser.getUserId(), nickname);

        // 2. UserOAuthAccount 엔티티 생성
        String email = extractEmail(userInfo);
        
        UserOAuthAccount oAuthAccount = UserOAuthAccount.builder()
                .user(savedUser)
                .email(email)
                .provider(OAuthProvider.KAKAO)
                .providerUserId(String.valueOf(userInfo.getId()))
                .providerEmail(email)
                .connectedAt(LocalDateTime.now())
                .build();

        UserOAuthAccount savedOAuthAccount = oAuthAccountRepository.save(oAuthAccount);
        log.info("OAuth 계정 연동 완료: provider=KAKAO, kakaoUserId={}", userInfo.getId());

        return savedOAuthAccount;
    }

    // 고유한 닉네임 생성
// 카카오 닉네임이 중복이면 뒤에 숫자 추가
    private String generateUniqueNickname(KakaoUserInfoResponse userInfo) {
        String baseNickname = "사용자";  // 기본값을 먼저 설정

        // Null-safe 체크: 각 단계마다 null 확인
        try {
            if (userInfo != null &&
                    userInfo.getKakaoAccount() != null &&
                    userInfo.getKakaoAccount().getProfile() != null) {

                String kakaoNickname = userInfo.getKakaoAccount().getProfile().getNickname();

                // 카카오 닉네임이 있고 비어있지 않으면 사용
                if (kakaoNickname != null && !kakaoNickname.trim().isEmpty()) {
                    baseNickname = kakaoNickname;
                }
            }
        } catch (Exception e) {
            log.warn("카카오 닉네임 추출 실패, 기본값 사용: {}", e.getMessage());
        }

        // 중복 체크 및 고유 닉네임 생성
        String nickname = baseNickname;
        int suffix = 1;
        while (userRepository.existsByNickname(nickname)) {
            nickname = baseNickname + suffix;
            suffix++;
        }

        log.info("생성된 닉네임: {}", nickname);
        return nickname;
    }

    // 이메일 추출 (없을 수 있음)
    private String extractEmail(KakaoUserInfoResponse userInfo) {
        if (userInfo.getKakaoAccount() != null) {
            String email = userInfo.getKakaoAccount().getEmail();
            // 이메일이 없으면 대표 이메일로 기본값 설정
            return (email != null && !email.isEmpty()) ? email : "no-email@scuad.kr";
        }
        return "no-email@scuad.kr";
    }

    // Refresh Token DB 저장
    // 기존 토큰이 있으면 업데이트, 없으면 새로 생성
    private void saveRefreshToken(User user, String refreshToken) {
        LocalDateTime expiresAt = LocalDateTime.now()
                .plusSeconds(jwtProperties.getRefreshTokenExpiration() / 1000);

        // 토큰을 해시 처리
        String hashedToken = TokenHasher.hash(refreshToken);
        log.debug("Refresh Token 해시 처리 완료: userId={}", user.getUserId());

        AuthRefreshToken tokenEntity = refreshTokenRepository
                .findByUser(user)
                .stream()
                .filter(AuthRefreshToken::isValid)
                .findFirst()
                .map(existing -> {
                    // 기존 토큰 업데이트 (해시 값 저장)
                    existing.updateToken(hashedToken, expiresAt);  // 해시 저장
                    return existing;
                })
                .orElseGet(() -> {
                    // 새 토큰 생성 (해시 값 저장)
                    return AuthRefreshToken.builder()
                            .user(user)
                            .tokenValue(hashedToken)  // 해시 저장
                            .expiresAt(expiresAt)
                            .build();
                });

        refreshTokenRepository.save(tokenEntity);
        log.info("Refresh Token 저장 완료 (해시): userId={}", user.getUserId());
    }

    // Refresh Token으로 Access Token 재발급
    // Refresh Token도 함께 갱신하여 보안 강화 (Refresh Token Rotation)
    @Transactional
    public TokenRefreshResponse refreshAccessToken(String refreshTokenValue) {
        // 1. 입력 토큰을 해시 처리
        String hashedToken = TokenHasher.hash(refreshTokenValue);
        log.debug("Refresh Token 해시 처리하여 조회 시작");

        // 2. DB에서 Refresh Token 조회 (해시 값으로 조회)
        Optional<AuthRefreshToken> tokenOpt = refreshTokenRepository
                .findByTokenValueAndRevokedAtIsNullAndExpiresAtAfter(
                        hashedToken,  // 해시 값으로 조회
                        LocalDateTime.now()
                );

        // 3. 해시로 조회 실패 시 평문으로 재시도 (하위 호환 - 기존 사용자 지원)
        if (tokenOpt.isEmpty()) {
            log.warn("해시로 조회 실패, 평문으로 재시도 (레거시 토큰)");
            tokenOpt = refreshTokenRepository
                    .findByTokenValueAndRevokedAtIsNullAndExpiresAtAfter(
                            refreshTokenValue,  // 평문으로 조회
                            LocalDateTime.now()
                    );

            if (tokenOpt.isPresent()) {
                log.info("레거시 평문 토큰 발견: userId={}, 다음 재발급 시 해시로 전환됨",
                        tokenOpt.get().getUser().getUserId());
            }
        }

        // 4. 둘 다 실패하면 에러
        AuthRefreshToken refreshToken = tokenOpt
                .orElseThrow(() -> {
                    log.warn("유효하지 않은 Refresh Token (해시/평문 모두 불일치 또는 만료)");
                    return new ApiException(ErrorCode.REFRESH_TOKEN_NOT_FOUND);
                });

        // 5. Refresh Token이 만료되었는지 검증
        if (!refreshToken.isValid()) {
            log.warn("만료된 Refresh Token: userId={}", refreshToken.getUser().getUserId());
            throw new ApiException(ErrorCode.REFRESH_TOKEN_EXPIRED);
        }

        User user = refreshToken.getUser();
        log.info("Refresh Token 검증 완료: userId={}", user.getUserId());

        // 6. 새 Access Token 발급
        String newAccessToken = jwtProvider.generateAccessToken(user.getUserId(), user.getRole().name());

        // 7. 새 Refresh Token 발급 (Refresh Token Rotation)
        String newRefreshToken = jwtProvider.generateRefreshToken();

        // 8. DB에 새 Refresh Token 저장 (해시로 저장)
        LocalDateTime newExpiresAt = LocalDateTime.now()
                .plusSeconds(jwtProperties.getRefreshTokenExpiration() / 1000);

        // 새 토큰을 해시 처리하여 저장
        String newHashedToken = TokenHasher.hash(newRefreshToken);
        refreshToken.updateToken(newHashedToken, newExpiresAt);  // 해시 저장
        refreshTokenRepository.save(refreshToken);

        log.info("토큰 재발급 완료 (해시로 저장): userId={}", user.getUserId());

        // 9. 응답 생성
        return TokenRefreshResponse.of(
                newAccessToken,
                newRefreshToken,  // 클라이언트에는 원본 토큰 전달 (쿠키용)
                jwtProperties.getAccessTokenExpiration() / 1000
        );
    }

    // 전체 디바이스 로그아웃
    // 해당 사용자의 모든 유효한 Refresh Token을 철회
    @Transactional
    public void logoutAllDevices(Long userId) {
        log.info("전체 디바이스 로그아웃 시작: userId={}", userId);

        // 해당 사용자의 모든 유효한 토큰 조회
        List<AuthRefreshToken> validTokens = refreshTokenRepository
                .findByUser_UserIdAndRevokedAtIsNull(userId);

        if (validTokens.isEmpty()) {
            log.warn("로그아웃할 유효한 토큰이 없음: userId={}", userId);
            return;
        }

        // 모든 토큰 철회
        LocalDateTime now = LocalDateTime.now();
        validTokens.forEach(token -> token.revoke(now));

        refreshTokenRepository.saveAll(validTokens);

        log.info("전체 디바이스 로그아웃 완료: userId={}, 철회된 토큰 수={}",
                userId, validTokens.size());
    }
}