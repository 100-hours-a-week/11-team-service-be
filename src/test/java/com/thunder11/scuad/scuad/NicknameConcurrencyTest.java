package com.thunder11.scuad.scuad;

import com.thunder11.scuad.auth.client.KakaoOAuthClient;
import com.thunder11.scuad.auth.dto.KakaoTokenResponse;
import com.thunder11.scuad.auth.dto.KakaoUserInfoResponse;
import com.thunder11.scuad.auth.repository.UserOAuthAccountRepository;
import com.thunder11.scuad.auth.repository.UserRepository;
import com.thunder11.scuad.auth.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@SpringBootTest
@ActiveProfiles("test")
class NicknameConcurrencyTest {

    @Autowired private AuthService authService;
    @Autowired private UserRepository userRepository;
    @Autowired private UserOAuthAccountRepository oAuthAccountRepository;
    @MockBean  private KakaoOAuthClient kakaoOAuthClient;

    @BeforeEach
    void setUp() {
        oAuthAccountRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("같은 카카오 닉네임으로 동시 로그인 시 race condition 발생")
    void 동시_로그인_닉네임_race_condition_재현() throws InterruptedException {
        int threadCount = 5;

        given(kakaoOAuthClient.getAccessToken(any())).willReturn(new KakaoTokenResponse());
        given(kakaoOAuthClient.getUserInfo(any())).willAnswer(inv ->
                buildMockUserInfo(Thread.currentThread().getId(), "레이스닉네임")
        );

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch  = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount    = new AtomicInteger(0);
        List<String>  errors       = new CopyOnWriteArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            final String code = "test-code-" + i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    authService.processKakaoCallback(code);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                    errors.add(e.getClass().getSimpleName() + ": " + e.getMessage());
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        System.out.println("=================================================");
        System.out.println("[이슈11] 닉네임 race condition 결과");
        System.out.println("  동시 요청 수  : " + threadCount);
        System.out.println("  성공          : " + successCount.get());
        System.out.println("  실패(예외)    : " + failCount.get());
        System.out.println("  저장된 유저 수: " + userRepository.count());
        errors.forEach(e -> System.out.println("    - " + e));
        System.out.println("=================================================");

        assertThat(successCount.get() + failCount.get()).isEqualTo(threadCount);
    }

    private KakaoUserInfoResponse buildMockUserInfo(long kakaoId, String nickname) {
        try {
            KakaoUserInfoResponse userInfo = new KakaoUserInfoResponse();
            set(userInfo, "id", kakaoId);

            var account = KakaoUserInfoResponse.KakaoAccount.class.getDeclaredConstructor().newInstance();
            var profile = KakaoUserInfoResponse.Profile.class.getDeclaredConstructor().newInstance();
            set(profile,  "nickname", nickname);
            set(account,  "profile",  profile);
            set(account,  "email",    "test" + kakaoId + "@test.com");
            set(userInfo, "kakaoAccount", account);
            return userInfo;
        } catch (Exception e) {
            throw new RuntimeException("Mock 생성 실패", e);
        }
    }

    private void set(Object obj, String field, Object value) throws Exception {
        var f = obj.getClass().getDeclaredField(field);
        f.setAccessible(true);
        f.set(obj, value);
    }
}