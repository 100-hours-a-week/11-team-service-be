package com.thunder11.scuad.scuad;

import com.thunder11.scuad.auth.domain.Role;
import com.thunder11.scuad.auth.domain.User;
import com.thunder11.scuad.auth.domain.UserStatus;
import com.thunder11.scuad.auth.repository.UserRepository;
import com.thunder11.scuad.chat.domain.ChatRoom;
import com.thunder11.scuad.chat.domain.ChatRoomMember;
import com.thunder11.scuad.chat.domain.type.MemberRole;
import com.thunder11.scuad.chat.domain.type.RoomGoal;
import com.thunder11.scuad.chat.repository.ChatRoomMemberRepository;
import com.thunder11.scuad.chat.repository.ChatRoomRepository;
import com.thunder11.scuad.chat.service.ChatMemberComparisonService;
import com.thunder11.scuad.infra.ai.client.AiServiceClient;
import com.thunder11.scuad.infra.ai.dto.response.AiCompareResponse;
import com.thunder11.scuad.jobposting.domain.Company;
import com.thunder11.scuad.jobposting.domain.JobApplication;
import com.thunder11.scuad.jobposting.domain.JobMaster;
import com.thunder11.scuad.jobposting.domain.type.ApplicationStatus;
import com.thunder11.scuad.jobposting.domain.type.JobStatus;
import com.thunder11.scuad.jobposting.repository.AiApplicantComparisonRepository;
import com.thunder11.scuad.jobposting.repository.CompanyRepository;
import com.thunder11.scuad.jobposting.repository.JobApplicationRepository;
import com.thunder11.scuad.jobposting.repository.JobMasterRepository;
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

/**
 * [이슈8] AI 비교 동시성 재현 테스트
 *
 * 목적:
 *   findBy → (AI 호출 500ms) → save() 사이의 race condition 재현
 *
 * 개선 전 예상 결과:
 *   DB 저장 건수 = 2 (중복) → assertThat(savedCount).isEqualTo(1) 실패
 *   이 실패가 곧 race condition 재현 성공을 의미함
 */
@SpringBootTest
@ActiveProfiles("test")
class ComparisonConcurrencyTest {

    @Autowired private ChatMemberComparisonService comparisonService;
    @Autowired private AiApplicantComparisonRepository comparisonRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private CompanyRepository companyRepository;
    @Autowired private JobMasterRepository jobMasterRepository;
    @Autowired private JobApplicationRepository jobApplicationRepository;
    @Autowired private ChatRoomRepository chatRoomRepository;
    @Autowired private ChatRoomMemberRepository chatRoomMemberRepository;

    @MockBean
    private AiServiceClient aiServiceClient;

    private Long chatRoomId;
    private Long myUserId;
    private Long targetMemberId;

    @BeforeEach
    void setUp() {
        // FK 역순 전체 삭제 (테스트 격리)
        comparisonRepository.deleteAll();
        chatRoomMemberRepository.deleteAll();
        chatRoomRepository.deleteAll();
        jobApplicationRepository.deleteAll();
        jobMasterRepository.deleteAll();
        companyRepository.deleteAll();
        userRepository.deleteAll();

        // 1. User 생성
        User myUser = userRepository.save(User.builder()
                .nickname("테스트유저1")
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .build());

        User competitorUser = userRepository.save(User.builder()
                .nickname("테스트유저2")
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .build());

        // 2. Company 생성
        Company company = companyRepository.save(new Company("테스트회사", "test.com"));

        // 3. JobMaster 생성
        JobMaster jobMaster = jobMasterRepository.save(JobMaster.builder()
                .company(company)
                .jobTitle("백엔드 개발자")
                .status(JobStatus.OPEN)
                .build());

        // 4. JobApplication 생성
        JobApplication myApplication = jobApplicationRepository.save(JobApplication.builder()
                .user(myUser)
                .jobMaster(jobMaster)
                .status(ApplicationStatus.ACTIVE)
                .build());

        JobApplication competitorApp = jobApplicationRepository.save(JobApplication.builder()
                .user(competitorUser)
                .jobMaster(jobMaster)
                .status(ApplicationStatus.ACTIVE)
                .build());

        // 5. ChatRoom 생성
        ChatRoom chatRoom = chatRoomRepository.save(ChatRoom.builder()
                .jobMasterId(jobMaster.getId())
                .createdBy(myUser.getUserId())
                .roomName("테스트채팅방")
                .maxParticipants(5)
                .roomGoal(RoomGoal.DOCUMENT)
                .cutlineScore(70)
                .build());

        // 6. ChatRoomMember 생성
        ChatRoomMember myMember = chatRoomMemberRepository.save(
                ChatRoomMember.builder()
                        .chatRoomId(chatRoom.getChatRoomId())
                        .userId(myUser.getUserId())
                        .jobApplicationId(myApplication.getId())
                        .role(MemberRole.HOST)
                        .build());

        ChatRoomMember targetMember = chatRoomMemberRepository.save(
                ChatRoomMember.builder()
                        .chatRoomId(chatRoom.getChatRoomId())
                        .userId(competitorUser.getUserId())
                        .jobApplicationId(competitorApp.getId())
                        .role(MemberRole.MEMBER)
                        .build());

        this.chatRoomId = chatRoom.getChatRoomId();
        this.myUserId = myUser.getUserId();
        this.targetMemberId = targetMember.getChatRoomMemberId();
    }

    @Test
    @DisplayName("[재현] 동시 요청 시 DB에 2건 중복 저장되는 race condition 확인")
    void 동시_비교_요청_race_condition_재현() throws InterruptedException {
        int threadCount = 2;

        // Thread.sleep(500): AI 호출 지연 시뮬레이션
        // → race condition 창을 수 μs → 500ms 로 강제 확장
        // → 두 스레드 모두 findBy 에서 "결과 없음"으로 통과하게 유도
        given(aiServiceClient.compareApplicants(any())).willAnswer(inv -> {
            Thread.sleep(500);
            return AiCompareResponse.builder()
                    .comparisonMetrics(List.of())
                    .strengthsReport("강점 테스트")
                    .weaknessesReport("약점 테스트")
                    .build();
        });

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch  = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount    = new AtomicInteger(0);
        List<String> errors = new CopyOnWriteArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await(); // 두 스레드 동시 출발 보장
                    comparisonService.compare(chatRoomId, myUserId, targetMemberId);
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

        long savedCount = comparisonRepository.count();

        System.out.println("=================================================");
        System.out.println("[이슈8] AI 비교 race condition 재현 결과");
        System.out.println("  동시 요청 수  : " + threadCount);
        System.out.println("  성공          : " + successCount.get());
        System.out.println("  실패(예외)    : " + failCount.get());
        System.out.println("  DB 저장 건수  : " + savedCount);
        errors.forEach(e -> System.out.println("    - " + e));
        System.out.println("=================================================");

        // 개선 전: savedCount = 2로 이 assert가 실패 → race condition 재현 확인
        assertThat(successCount.get()).isEqualTo(threadCount);
        assertThat(failCount.get()).isEqualTo(0);
        assertThat(savedCount).isEqualTo(1L);
    }
}