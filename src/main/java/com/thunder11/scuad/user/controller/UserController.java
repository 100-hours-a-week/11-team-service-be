package com.thunder11.scuad.user.controller;

import com.thunder11.scuad.user.dto.request.UserWithdrawalRequest;
import com.thunder11.scuad.user.dto.request.UserUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.thunder11.scuad.auth.security.UserPrincipal;
import com.thunder11.scuad.common.response.ApiResponse;
import com.thunder11.scuad.user.dto.UserResponse;
import com.thunder11.scuad.user.service.UserService;

import lombok.RequiredArgsConstructor;


// User 도메인 컨트롤러
// 사용자 정보 조회 및 관리 API
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;


     // 현재 로그인한 사용자 정보 조회
     // GET /api/v1/users/me
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

    // 회원정보 수정
    // PATCH /api/v1/users/me
    // 변경하지 않을 필드는 요청 body에 포함하지 않습니다
    // email은 수정 불가(read-only)이며 요청에 포함해도 무시됩니다
    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> updateCurrentUser(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody UserUpdateRequest request
    ) {
        UserResponse userResponse = userService.updateUser(userPrincipal.getUserId(), request);

        return ResponseEntity.ok(
                ApiResponse.of(
                        200,
                        "USER_002",
                        "사용자 정보를 성공적으로 수정했습니다.",
                        userResponse
                )
        );
    }

    // 회원 탈퇴
    // DELETE /api/v1/users/me
    // 탈퇴 완료 후 클라이언트는 로컬 토큰을 삭제하고 로그인 화면으로 이동해야 합니다
    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> withdrawCurrentUser(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody UserWithdrawalRequest request
    ) {
        userService.withdrawUser(userPrincipal.getUserId(), request);

        return ResponseEntity.ok(
                ApiResponse.of(
                        200,
                        "USER_003",
                        "회원 탈퇴가 완료되었습니다."
                )
        );
    }
}
