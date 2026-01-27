package com.thunder11.scuad.common.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;

@Getter
public enum ErrorCode {

    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "요청 값이 올바르지 않습니다."),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "잘못된 요청입니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "NOT_FOUND", "리소스를 찾을 수 없습니다."),
    CONFLICT(HttpStatus.CONFLICT, "CONFLICT", "이미 존재하는 리소스입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "FORBIDDEN", "권한이 없습니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "서버 오류가 발생했습니다."),

    // oauth 도메인 에러
    INVALID_OAUTH_CODE(HttpStatus.BAD_REQUEST, "INVALID_OAUTH_CODE", "유효하지 않은 OAuth 인가 코드입니다."),
    OAUTH_PROVIDER_ERROR(HttpStatus.BAD_GATEWAY, "OAUTH_PROVIDER_ERROR", "OAuth 제공자 서버와의 통신에 실패했습니다."),
    ACCESS_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "ACCESS_TOKEN_EXPIRED", "인증 토큰이 만료되었습니다. 토큰을 재발급 받아주세요."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "유효하지 않은 인증 토큰입니다."),
    REFRESH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "REFRESH_TOKEN_EXPIRED", "리프레시 토큰이 만료되었습니다. 다시 로그인해주세요."),
    REFRESH_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "REFRESH_TOKEN_NOT_FOUND", "리프레시 토큰을 찾을 수 없습니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "사용자를 찾을 수 없습니다."),
    KAKAO_TOKEN_REQUEST_FAILED(HttpStatus.BAD_REQUEST, "KAKAO_TOKEN_REQUEST_FAILED", "카카오 토큰 발급에 실패했습니다."),
    KAKAO_API_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "KAKAO_API_ERROR", "카카오 API 호출 중 오류가 발생했습니다."),
    AI_SERVICE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "AI_SERVICE_ERROR", "AI 서비스 연동 중 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}
