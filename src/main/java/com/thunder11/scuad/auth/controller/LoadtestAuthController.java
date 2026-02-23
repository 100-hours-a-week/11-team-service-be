package com.thunder11.scuad.auth.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

import com.thunder11.scuad.auth.dto.LoadtestTokenBatchResponse;
import com.thunder11.scuad.auth.dto.LoadtestTokenIssueResponse;
import com.thunder11.scuad.auth.service.LoadtestAuthService;
import com.thunder11.scuad.common.exception.ApiException;
import com.thunder11.scuad.common.exception.ErrorCode;
import com.thunder11.scuad.common.response.ApiResponse;

@Profile("loadtest")
@RestController
@RequestMapping("/api/v1/auth/test")
@RequiredArgsConstructor
public class LoadtestAuthController {

    private final LoadtestAuthService loadtestAuthService;

    @Value("${loadtest.secret}")
    private String loadtestSecret;

    @GetMapping("/tokens")
    public ApiResponse<LoadtestTokenBatchResponse> issueTokens(
            @RequestHeader(value = "X-Test-Secret", required = false) String secret,
            @RequestParam(defaultValue = "1") int count) {

        if (secret == null || secret.isBlank() || !secret.equals(loadtestSecret)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "X-Test-Secret이 올바르지 않습니다.");
        }
        if (count < 1 || count > 5000) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "count는 1~5000 범위여야 합니다.");
        }

        List<LoadtestTokenIssueResponse> tokens = loadtestAuthService.issueTokens(count);

        LoadtestTokenBatchResponse payload = LoadtestTokenBatchResponse.builder()
                .count(tokens.size())
                .tokens(tokens)
                .build();

        return ApiResponse.of(
                HttpStatus.OK.value(),
                "LOADTEST_TOKEN_ISSUED",
                "loadtest용 access token을 발급했습니다.",
                payload);
    }
}
