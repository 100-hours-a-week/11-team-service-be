package com.thunder11.scuad.auth.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import com.thunder11.scuad.auth.domain.Role;
import com.thunder11.scuad.auth.domain.User;
import com.thunder11.scuad.auth.domain.UserStatus;
import com.thunder11.scuad.auth.dto.LoadtestTokenIssueResponse;
import com.thunder11.scuad.auth.repository.UserRepository;
import com.thunder11.scuad.auth.util.JwtProvider;

@Service
@RequiredArgsConstructor
public class LoadtestAuthService {

    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;

    @Transactional
    public List<LoadtestTokenIssueResponse> issueTokens(int count) {
        List<LoadtestTokenIssueResponse> result = new ArrayList<>(count);
        long ts = Instant.now().toEpochMilli();

        for (int i = 0; i < count; i++) {
            String nickname = generateUniqueNickname(ts, i);

            User user = User.builder()
                    .nickname(nickname)
                    .role(Role.USER)
                    .status(UserStatus.ACTIVE)
                    .build();

            User saved = userRepository.save(user);
            String accessToken = jwtProvider.generateAccessToken(saved.getUserId(), saved.getRole().name());

            result.add(LoadtestTokenIssueResponse.builder()
                    .userId(saved.getUserId())
                    .nickname(saved.getNickname())
                    .accessToken(accessToken)
                    .tokenType("Bearer")
                    .build());
        }

        return result;
    }

    private String generateUniqueNickname(long ts, int i) {
        // nickname unique 제약이 있으므로 충돌 가능성을 극도로 낮춤
        // 길이 제한 30이므로 길이를 신경써서 구성
        // 예: lt_1700000000000_1_a1b2
        String base = "lt_" + ts + "_" + i + "_" + UUID.randomUUID().toString().substring(0, 4);
        String nickname = base.length() > 30 ? base.substring(0, 30) : base;

        // 혹시 모를 충돌 대비 (대부분 1회 통과)
        int retry = 0;
        while (userRepository.existsByNickname(nickname) && retry < 5) {
            String alt = "lt_" + ts + "_" + i + "_" + UUID.randomUUID().toString().substring(0, 4);
            nickname = alt.length() > 30 ? alt.substring(0, 30) : alt;
            retry++;
        }
        return nickname;
    }
}
