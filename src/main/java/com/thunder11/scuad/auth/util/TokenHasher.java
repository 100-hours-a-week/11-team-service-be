package com.thunder11.scuad.auth.util;

import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


// Refresh Token 해싱 유틸리티
// SHA-256 알고리즘을 사용하여 토큰을 해시 처리
@Slf4j
public class TokenHasher {

    private static final String ALGORITHM = "SHA-256";

    // 토큰을 SHA-256으로 해시 처리
    public static String hash(String token) {
        if (token == null || token.isEmpty()) {
            throw new IllegalArgumentException("토큰은 null이거나 빈 문자열일 수 없습니다.");
        }

        try {
            // SHA-256 MessageDigest 인스턴스 생성
            MessageDigest digest = MessageDigest.getInstance(ALGORITHM);

            // 토큰을 바이트 배열로 변환하여 해시 계산
            byte[] hashBytes = digest.digest(token.getBytes(StandardCharsets.UTF_8));

            // 바이트 배열을 16진수 문자열로 변환
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            String hashedToken = hexString.toString();
            log.debug("토큰 해시 생성 완료: 원본 길이={}, 해시 길이={}", token.length(), hashedToken.length());

            return hashedToken;

        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256 알고리즘을 찾을 수 없습니다.", e);
            throw new RuntimeException("토큰 해싱 실패: " + e.getMessage(), e);
        }
    }

    /// 원본 토큰과 해시된 토큰이 일치하는지 확인
    public static boolean matches(String rawToken, String hashedToken) {
        if (rawToken == null || hashedToken == null) {
            return false;
        }

        try {
            String newHash = hash(rawToken);
            boolean isMatch = newHash.equals(hashedToken);

            log.debug("토큰 해시 비교: 일치={}", isMatch);
            return isMatch;

        } catch (Exception e) {
            log.error("토큰 해시 비교 실패", e);
            return false;
        }
    }
}