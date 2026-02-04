package com.thunder11.scuad.user.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.thunder11.scuad.auth.security.UserPrincipal;
import com.thunder11.scuad.common.response.ApiResponse;
import com.thunder11.scuad.user.dto.UserResponse;
import com.thunder11.scuad.user.service.UserService;

import lombok.RequiredArgsConstructor;

/**
 * User 도메인 컨트롤러
 * 사용자 정보 조회 및 관리 API
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 현재 로그인한 사용자 정보 조회
     * 
     * GET /api/v1/users/me
     * 
     * @param userPrincipal JWT에서 추출한 사용자 인증 정보
     * @return ResponseEntity<ApiResponse<UserResponse>> 사용자 정보
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        UserResponse userResponse = userService.getCurrentUser(userPrincipal.getUserId());

        return ResponseEntity.ok(
                ApiResponse.of(
                        200,
                        "USER_001",
                        "사용자 정보를 성공적으로 조회했습니다.",
                        userResponse
                )
        );
    }
}
