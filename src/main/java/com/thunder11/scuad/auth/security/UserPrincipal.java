package com.thunder11.scuad.auth.security;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import com.thunder11.scuad.auth.domain.Role;

// 인증된 사용자 정보를 담는 객체
// SecurityContext에 저장되어 컨트롤러에서 사용
@Getter
@RequiredArgsConstructor
public class UserPrincipal {

    private final Long userId;
    private final Role role;

    public static UserPrincipal of(Long userId, Role role) {
        return new UserPrincipal(userId, role);
    }
}