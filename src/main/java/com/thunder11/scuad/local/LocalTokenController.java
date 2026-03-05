package com.thunder11.scuad.local;

import com.thunder11.scuad.auth.util.JwtProvider;
import com.thunder11.scuad.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * [로컬 전용] 테스트용 JWT 발급 컨트롤러
 *
 * 도입 의도:
 *   k6, Postman 테스트 시 카카오 OAuth 로그인 없이 바로 JWT를 발급받기 위함.
 *   @Profile("local")로 제한해 dev/prod 환경에서는 빈 자체가 등록되지 않으므로
 *   보안 위협 없음.
 *
 * 사용법:
 *   GET /local/token?userId=4  → userId=4의 ACCESS TOKEN 반환
 *   userId는 V2/V5 시드 데이터의 user_id와 일치해야 함
 */
@Profile("local")
@RestController
@RequestMapping("/local")
@RequiredArgsConstructor
public class LocalTokenController {

    private final JwtProvider jwtProvider;

    @GetMapping("/token")
    public ApiResponse<String> getTestToken(@RequestParam Long userId) {
        // role은 USER로 고정 (시드 데이터 기준)
        String token = jwtProvider.generateAccessToken(userId, "USER");

        return ApiResponse.of(
                HttpStatus.OK.value(),
                "SUCCESS",
                "테스트 토큰 발급 완료 (로컬 전용)",
                token
        );
    }
}