package com.thunder11.scuad.auth.client;

import com.thunder11.scuad.auth.config.KakaoProperties;
import com.thunder11.scuad.auth.dto.KakaoTokenResponse;
import com.thunder11.scuad.auth.dto.KakaoUserInfoResponse;
import com.thunder11.scuad.common.exception.ApiException;
import com.thunder11.scuad.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

// 카카오 OAuth API 통신 클라이언트
// RestTemplate을 사용해 카카오 서버와 HTTP 통신 수행
@Slf4j
@Component
@RequiredArgsConstructor
public class KakaoOAuthClient {

    private final RestTemplate restTemplate;
    private final KakaoProperties kakaoProperties;

    // 카카오 인가 코드로 액세스 토큰 발급
    // Authorization Code Grant 방식의 토큰 교환 단계
    public KakaoTokenResponse getAccessToken(String code) {
        // 요청 헤더 설정
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        // 요청 바디 설정 (application/x-www-form-urlencoded 형식)
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("client_id", kakaoProperties.getClientId());
        body.add("client_secret", kakaoProperties.getClientSecret());
        body.add("redirect_uri", kakaoProperties.getRedirectUri());
        body.add("code", code);  // 카카오로부터 받은 인가 코드

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        try {
            // POST 요청으로 토큰 발급
            ResponseEntity<KakaoTokenResponse> response = restTemplate.postForEntity(
                    kakaoProperties.getTokenUri(),
                    request,
                    KakaoTokenResponse.class
            );

            log.info("카카오 토큰 발급 성공");
            return response.getBody();

        } catch (RestClientException e) {
            log.error("카카오 토큰 발급 실패: {}", e.getMessage());
            throw new ApiException(ErrorCode.KAKAO_TOKEN_REQUEST_FAILED);
        }
    }

    // 카카오 액세스 토큰으로 사용자 정보 조회
    // 카카오 회원번호, 이메일, 프로필 정보 획득
    public KakaoUserInfoResponse getUserInfo(String accessToken) {
        // 요청 헤더 설정 (Bearer 인증)
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            // GET 요청으로 사용자 정보 조회
            ResponseEntity<KakaoUserInfoResponse> response = restTemplate.exchange(
                    kakaoProperties.getUserInfoUri(),
                    HttpMethod.GET,
                    request,
                    KakaoUserInfoResponse.class
            );

            log.info("카카오 사용자 정보 조회 성공: userId={}", response.getBody().getId());
            return response.getBody();

        } catch (RestClientException e) {
            log.error("카카오 사용자 정보 조회 실패: {}", e.getMessage());
            throw new ApiException(ErrorCode.KAKAO_API_ERROR);
        }
    }
}