package com.thunder11.scuad.auth;

import com.thunder11.scuad.auth.domain.OAuthProvider;
import com.thunder11.scuad.auth.domain.UserOAuthAccount;
import com.thunder11.scuad.auth.repository.UserOAuthAccountRepository;
import com.thunder11.scuad.auth.service.UserRegistrationHelper;
import com.thunder11.scuad.config.TestInfraConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * [개선 전 관측용] 카카오 OAuth 동시 가입 Race Condition 재현 테스트
 *
 * <p>목적: 동일한 providerUserId로 동시에 신규 가입이 시도될 때
 * user_oauth_accounts 테이블에 중복 row가 삽입되는 현상을 재현한다.
 *
 * <p>재현 조건:
 * - user_oauth_accounts 테이블에 (provider, provider_user_id) UNIQUE 제약 없음 (현재 상태)
 * - tryCreateUser()가 동시에 여러 스레드에서 동일한 providerUserId로 호출됨
 *
 * <p>예상 결과 (개선 전):
 * - 여러 스레드가 모두 INSERT 성공 → user_oauth_accounts 중복 row 발생
 * - 이후 findByProviderAndProviderUserId() 호출 시 IncorrectResultSizeDataAccessException 발생
 *
 * <p>테스트 인프라:
 * - DB          : H2 인메모리 (application-test.yml)
 * - Redis       : Mock (TestInfraConfig)
 * - RabbitMQ    : autoconfigure 제외 (application-test.yml)
 * - AWS S3      : autoconfigure 제외 + Mock (application-test.yml + TestInfraConfig)
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestInfraConfig.class)
class OAuthConcurrencyTest {

    private static final Logger log = LoggerFactory.getLogger(OAuthConcurrencyTest.class);

    @Autowired
    private UserRegistrationHelper userRegistrationHelper;

    @Autowired
    private UserOAuthAccountRepository oAuthAccountRepository;

    // 동시 가입 테스트용 고정 kakaoUserId (실제 카카오 ID 형식)
    private static final String RACE_KAKAO_USER_ID = "9999999999";
    private static final int THREAD_COUNT = 5;

    @AfterEach
    void cleanUp() {
        // 테스트 격리: 매 테스트 후 삽입된 데이터 전체 제거
        // user_oauth_accounts → users 순서로 FK 제약 고려하여 삭제
        oAuthAccountRepository.deleteAll();
    }

    // =========================================================================
    // 테스트 1: 중복 row 삽입 관측
    // =========================================================================
    @Test
    @DisplayName("[이슈] OAuth 동시 가입 Race Condition - 중복 row 발생 관측")
    void oauthConcurrencyRaceCondition_shouldProduceDuplicateRows() throws InterruptedException {
        // =====================================================================
        // Given
        // 동일한 providerUserId로 동시에 신규 가입을 시도하는 스레드 5개 준비
        // 닉네임은 스레드별로 다르게 설정 (닉네임 충돌은 별개 이슈이므로 분리)
        // =====================================================================
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch startLatch = new CountDownLatch(1);       // 모든 스레드 동시 출발
        CountDownLatch doneLatch  = new CountDownLatch(THREAD_COUNT); // 전체 완료 대기

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount    = new AtomicInteger(0);
        List<String>  errors       = Collections.synchronizedList(new ArrayList<>());

        // =====================================================================
        // When
        // THREAD_COUNT개 스레드가 동시에 동일한 providerUserId로 tryCreateUser() 호출
        // =====================================================================
        for (int i = 0; i < THREAD_COUNT; i++) {
            final int threadIndex = i;
            executor.submit(() -> {
                try {
                    startLatch.await(); // 모든 스레드가 준비될 때까지 대기 후 동시 출발

                    userRegistrationHelper.tryCreateUser(
                            "레이스유저" + threadIndex,          // 닉네임: 스레드별 고유
                            "race" + threadIndex + "@test.com", // 이메일: 스레드별 고유
                            RACE_KAKAO_USER_ID,                 // ← 모든 스레드 동일한 kakaoUserId
                            OAuthProvider.KAKAO
                    );

                    successCount.incrementAndGet();
                    log.info("[Thread-{}] tryCreateUser 성공", threadIndex);

                } catch (Exception e) {
                    failCount.incrementAndGet();
                    String msg = "[Thread-" + threadIndex + "] "
                            + e.getClass().getSimpleName() + ": " + e.getMessage();
                    errors.add(msg);
                    log.warn("[Thread-{}] tryCreateUser 실패: {}", threadIndex, e.getMessage());
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // 동시 출발 신호
        doneLatch.await();      // 모든 스레드 완료 대기
        executor.shutdown();

        // =====================================================================
        // Then - 결과 집계 및 콘솔 출력 (스크린샷 대상)
        // =====================================================================
        List<UserOAuthAccount> savedAccounts = oAuthAccountRepository.findAll()
                .stream()
                .filter(a -> RACE_KAKAO_USER_ID.equals(a.getProviderUserId()))
                .toList();

        int savedCount = savedAccounts.size();

        // --- 스크린샷 대상 콘솔 출력 START ---
        System.out.println();
        System.out.println("=================================================");
        System.out.println("[이슈] OAuth 동시 가입 race condition 결과");
        System.out.println("  동시 요청 수        : " + THREAD_COUNT);
        System.out.println("  성공                : " + successCount.get());
        System.out.println("  실패(예외)          : " + failCount.get());
        System.out.println("  저장된 OAuth 계정 수: " + savedCount
                + (savedCount > 1 ? " ← ★ 중복 row 발생!" : " (중복 없음)"));
        if (!errors.isEmpty()) {
            System.out.println("  발생 예외:");
            errors.forEach(e -> System.out.println("    - " + e));
        }
        System.out.println("=================================================");
        System.out.println();
        // --- 스크린샷 대상 콘솔 출력 END ---

        if (savedCount > 1) {
            log.warn("★ Race Condition 재현 성공: {}개의 중복 OAuth row 삽입됨", savedCount);
        } else {
            log.info("중복 없이 처리됨 (REQUIRES_NEW 트랜잭션 직렬화가 우연히 발생했을 가능성)");
            log.info("→ 재실행하거나 THREAD_COUNT를 늘려서 재시도");
        }

        // 저장 수가 1 이상인지만 확인 (재현 여부와 무관하게 최소 1명은 저장되어야 함)
        org.assertj.core.api.Assertions.assertThat(savedCount).isGreaterThanOrEqualTo(1);
    }

    // =========================================================================
    // 테스트 2: 중복 row 존재 후 단건 조회 시 예외 발생 관측
    // =========================================================================
    @Test
    @DisplayName("[이슈 재현] 중복 row 삽입 후 findBy 단건 조회 시 IncorrectResultSizeDataAccessException 발생")
    void afterDuplicateInsert_findUnique_shouldThrowException() throws InterruptedException {
        // =====================================================================
        // Given
        // 동시 가입으로 중복 row가 이미 삽입된 상황을 시뮬레이션
        // =====================================================================
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch  = new CountDownLatch(THREAD_COUNT);

        for (int i = 0; i < THREAD_COUNT; i++) {
            final int idx = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    userRegistrationHelper.tryCreateUser(
                            "중복유저" + idx,
                            "dup" + idx + "@test.com",
                            RACE_KAKAO_USER_ID,
                            OAuthProvider.KAKAO
                    );
                } catch (Exception ignored) {
                    // 일부 실패 허용 — 중복 row 삽입 관측이 목적
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await();
        executor.shutdown();

        // =====================================================================
        // When & Then
        // 중복 row가 2개 이상이면 findByProviderAndProviderUserId() 호출 시 예외 발생 관측
        // =====================================================================
        long duplicateCount = oAuthAccountRepository.findAll()
                .stream()
                .filter(a -> RACE_KAKAO_USER_ID.equals(a.getProviderUserId()))
                .count();

        // --- 스크린샷 대상 콘솔 출력 START ---
        System.out.println();
        System.out.println("=================================================");
        System.out.println("[이슈 재현] 중복 row 삽입 후 단건 조회 결과");
        System.out.println("  DB에 저장된 동일 providerUserId row 수: " + duplicateCount);

        if (duplicateCount > 1) {
            System.out.println("  → ★ 중복 row 존재. findByProviderAndProviderUserId() 호출 시도...");
            try {
                oAuthAccountRepository
                        .findByProviderAndProviderUserId(OAuthProvider.KAKAO, RACE_KAKAO_USER_ID);
                System.out.println("  → 예외 미발생 (DB 드라이버가 첫 번째 row를 반환한 케이스)");
            } catch (Exception e) {
                System.out.println("  → ★ 예외 발생 확인!");
                System.out.println("    예외 타입: " + e.getClass().getSimpleName());
                System.out.println("    메시지  : " + e.getMessage());
                log.error("★ 재현 성공 - 실제 서비스에서 500 응답이 반환되는 지점", e);
            }
        } else {
            System.out.println("  → 중복 row 없음 (race condition 미발생 또는 이미 개선된 상태)");
        }

        System.out.println("=================================================");
        System.out.println();
        // --- 스크린샷 대상 콘솔 출력 END ---
    }
}
