-- =====================================================================
-- [로컬 개발 전용] 성능 테스트용 시드 데이터
-- 목적: Postman/k6 테스트 시 카카오 로그인 없이도 실제 데이터로 테스트 가능하도록
-- 주의: 이 파일은 application-local.yml의 testdata 경로에서만 로딩됨
-- 실행 순서: FK 의존성 순서를 지켜서 작성 (부모 테이블 → 자식 테이블)
--
-- 적용 순서: V1(테이블 생성) → V2(시드 데이터) → V3(UNIQUE 제약 변경)
-- =====================================================================

-- ① 유저 2명 생성 (user_id: 1=HOST, 2=MEMBER)
INSERT INTO users (user_id, profile_image_file_id, role, nickname, status, created_at, updated_at)
VALUES
    (1, NULL, 'USER', 'test-host',   'ACTIVE', NOW(), NOW()),
    (2, NULL, 'USER', 'test-member', 'ACTIVE', NOW(), NOW());

-- ② OAuth 계정 연결
INSERT INTO user_oauth_accounts (user_id, email, provider, provider_user_id, provider_email, connected_at, created_at, updated_at)
VALUES
    (1, 'host@test.com',   'KAKAO', 'kakao_test_1', 'host@test.com',   NOW(), NOW(), NOW()),
    (2, 'member@test.com', 'KAKAO', 'kakao_test_2', 'member@test.com', NOW(), NOW(), NOW());

-- ③ 회사 생성
INSERT INTO companies (company_id, name, domain, created_at, updated_at)
VALUES (1, '테스트주식회사', 'test.com', NOW(), NOW());

-- ④ 채용공고(job_master) 생성
INSERT INTO job_masters (job_master_id, company_id, job_title, status, created_at, updated_at)
VALUES (1, 1, '백엔드 개발자', 'OPEN', NOW(), NOW());

-- ⑤ job_post 생성
INSERT INTO job_posts (job_post_id, job_master_id, ai_job_id, company_id, source_type, source_url, source_url_hash,
                       raw_company_name, raw_job_title, recruitment_status, registration_status, fingerprint_hash, created_at, updated_at)
VALUES (1, 1, 1, 1, 'MANUAL', 'https://test.com/job/1', SHA2('https://test.com/job/1', 256),
        '테스트주식회사', '백엔드 개발자', 'OPEN', 'REGISTERED', SHA2('test-fingerprint-1', 256), NOW(), NOW());

-- ⑥ 지원 이력
INSERT INTO job_applications (job_application_id, user_id, job_master_id, status, created_at, updated_at)
VALUES
    (1, 1, 1, 'SUBMITTED', NOW(), NOW()),
    (2, 2, 1, 'SUBMITTED', NOW(), NOW());

-- ⑦ AI 평가 결과 (cutline_score 통과용)
INSERT INTO ai_applicant_evaluation (evaluation_id, job_application_id, overall_score,
                                     one_line_review, feedback_detail, comparison_scores, created_at, updated_at)
VALUES
    (1, 1, 90, '우수한 백엔드 개발자입니다', '매우 뛰어난 기술력을 보유하고 있습니다', '{}', NOW(), NOW()),
    (2, 2, 85, '훌륭한 개발 역량을 보유했습니다', '실력 있는 백엔드 개발자입니다', '{}', NOW(), NOW());

-- ⑧ 채팅방 생성
INSERT INTO chat_rooms (chat_room_id, job_master_id, created_by, room_name, max_participants,
                        room_goal, cutline_score, preferred_conditions, status, created_at, updated_at)
VALUES (1, 1, 1, '백엔드 스터디방', 10, 'INTERVIEW', 80, NULL, 'ACTIVE', NOW(), NOW());

-- ⑨ 채팅방 멤버
INSERT INTO chat_room_members (chat_room_member_id, chat_room_id, user_id, job_application_id, role, joined_at)
VALUES
    (1, 1, 1, 1, 'HOST',   NOW()),
    (2, 1, 2, 2, 'MEMBER', NOW());

-- ⑩ 채팅 메시지 50개 (폴링 성능 측정용)
INSERT INTO chat_messages (message_id, chat_room_id, sender_id, file_id, message_type, content, sent_at)
VALUES
    ( 1, 1, 1, NULL, 'TEXT', '안녕하세요! 스터디 시작해봐요',                   DATE_SUB(NOW(), INTERVAL 50 MINUTE)),
    ( 2, 1, 2, NULL, 'TEXT', '반갑습니다! 잘 부탁드립니다',                     DATE_SUB(NOW(), INTERVAL 49 MINUTE)),
    ( 3, 1, 1, NULL, 'TEXT', '오늘 준비한 주제는 JVM GC 입니다',                DATE_SUB(NOW(), INTERVAL 48 MINUTE)),
    ( 4, 1, 2, NULL, 'TEXT', 'GC 튜닝 경험이 있으신가요?',                      DATE_SUB(NOW(), INTERVAL 47 MINUTE)),
    ( 5, 1, 1, NULL, 'TEXT', '네 G1GC에서 ZGC로 전환한 경험이 있어요',         DATE_SUB(NOW(), INTERVAL 46 MINUTE)),
    ( 6, 1, 2, NULL, 'TEXT', '오 멋지네요 레이턴시가 줄었나요?',                DATE_SUB(NOW(), INTERVAL 45 MINUTE)),
    ( 7, 1, 1, NULL, 'TEXT', '네 p99가 30% 정도 개선됐어요',                    DATE_SUB(NOW(), INTERVAL 44 MINUTE)),
    ( 8, 1, 2, NULL, 'TEXT', '저는 인덱스 최적화 공부중입니다',                 DATE_SUB(NOW(), INTERVAL 43 MINUTE)),
    ( 9, 1, 1, NULL, 'TEXT', '커버링 인덱스 아시나요?',                         DATE_SUB(NOW(), INTERVAL 42 MINUTE)),
    (10, 1, 2, NULL, 'TEXT', '이름은 알지만 실제론 안써봤어요',                 DATE_SUB(NOW(), INTERVAL 41 MINUTE)),
    (11, 1, 1, NULL, 'TEXT', 'SELECT 절 컬럼을 인덱스로만 구성하는 기법이에요', DATE_SUB(NOW(), INTERVAL 40 MINUTE)),
    (12, 1, 2, NULL, 'TEXT', '아 그래서 테이블 접근을 아예 안하는거군요',       DATE_SUB(NOW(), INTERVAL 39 MINUTE)),
    (13, 1, 1, NULL, 'TEXT', '맞아요 풀스캔보다 훨씬 빠릅니다',                 DATE_SUB(NOW(), INTERVAL 38 MINUTE)),
    (14, 1, 2, NULL, 'TEXT', '저도 테스트해봐야겠어요',                         DATE_SUB(NOW(), INTERVAL 37 MINUTE)),
    (15, 1, 1, NULL, 'TEXT', 'EXPLAIN으로 먼저 실행계획 확인해보세요',          DATE_SUB(NOW(), INTERVAL 36 MINUTE)),
    (16, 1, 2, NULL, 'TEXT', 'type이 ALL이면 풀스캔이죠?',                      DATE_SUB(NOW(), INTERVAL 35 MINUTE)),
    (17, 1, 1, NULL, 'TEXT', '네 ref나 range가 나와야 좋습니다',                DATE_SUB(NOW(), INTERVAL 34 MINUTE)),
    (18, 1, 2, NULL, 'TEXT', '오늘 많이 배웠습니다 감사해요',                   DATE_SUB(NOW(), INTERVAL 33 MINUTE)),
    (19, 1, 1, NULL, 'TEXT', '다음 주제는 뭘로 할까요?',                        DATE_SUB(NOW(), INTERVAL 32 MINUTE)),
    (20, 1, 2, NULL, 'TEXT', '트랜잭션 격리 수준 어떤가요?',                    DATE_SUB(NOW(), INTERVAL 31 MINUTE)),
    (21, 1, 1, NULL, 'TEXT', '좋아요 MVCC랑 같이 보면 좋을 것 같아요',         DATE_SUB(NOW(), INTERVAL 30 MINUTE)),
    (22, 1, 2, NULL, 'TEXT', 'PHANTOM READ는 RR에서도 발생하나요?',             DATE_SUB(NOW(), INTERVAL 29 MINUTE)),
    (23, 1, 1, NULL, 'TEXT', 'MySQL InnoDB는 넥스트키락으로 방어합니다',        DATE_SUB(NOW(), INTERVAL 28 MINUTE)),
    (24, 1, 2, NULL, 'TEXT', '아 갭락이라는 것도 있던데요',                     DATE_SUB(NOW(), INTERVAL 27 MINUTE)),
    (25, 1, 1, NULL, 'TEXT', '네 갭락과 레코드락의 조합이에요',                 DATE_SUB(NOW(), INTERVAL 26 MINUTE)),
    (26, 1, 2, NULL, 'TEXT', '데드락은 어떻게 해결하시나요?',                   DATE_SUB(NOW(), INTERVAL 25 MINUTE)),
    (27, 1, 1, NULL, 'TEXT', '락 순서를 통일하거나 타임아웃 설정해요',          DATE_SUB(NOW(), INTERVAL 24 MINUTE)),
    (28, 1, 2, NULL, 'TEXT', '실전에서 데드락 만나면 무섭겠어요',               DATE_SUB(NOW(), INTERVAL 23 MINUTE)),
    (29, 1, 1, NULL, 'TEXT', '로그 보면 어떤 트랜잭션이 충돌인지 나와요',       DATE_SUB(NOW(), INTERVAL 22 MINUTE)),
    (30, 1, 2, NULL, 'TEXT', '오늘도 알찬 시간이었습니다!',                     DATE_SUB(NOW(), INTERVAL 21 MINUTE)),
    (31, 1, 1, NULL, 'TEXT', '다음엔 Redis 캐싱 전략 얘기해봐요',               DATE_SUB(NOW(), INTERVAL 20 MINUTE)),
    (32, 1, 2, NULL, 'TEXT', 'Cache Aside 패턴 공부해올게요',                   DATE_SUB(NOW(), INTERVAL 19 MINUTE)),
    (33, 1, 1, NULL, 'TEXT', 'Write Through도 같이 보면 좋아요',                DATE_SUB(NOW(), INTERVAL 18 MINUTE)),
    (34, 1, 2, NULL, 'TEXT', '캐시 무효화가 제일 어렵다고 하더라고요',          DATE_SUB(NOW(), INTERVAL 17 MINUTE)),
    (35, 1, 1, NULL, 'TEXT', 'TTL 설정이 핵심이에요',                           DATE_SUB(NOW(), INTERVAL 16 MINUTE)),
    (36, 1, 2, NULL, 'TEXT', '이벤트 기반 무효화도 있죠?',                      DATE_SUB(NOW(), INTERVAL 15 MINUTE)),
    (37, 1, 1, NULL, 'TEXT', 'Pub/Sub으로 캐시 동기화하는 방식이요',            DATE_SUB(NOW(), INTERVAL 14 MINUTE)),
    (38, 1, 2, NULL, 'TEXT', '분산 환경에서 중요하겠네요',                      DATE_SUB(NOW(), INTERVAL 13 MINUTE)),
    (39, 1, 1, NULL, 'TEXT', '맞아요 단일 서버는 간단하게 가능해요',            DATE_SUB(NOW(), INTERVAL 12 MINUTE)),
    (40, 1, 2, NULL, 'TEXT', '오늘 공부 내용 정리해서 올려드릴게요',            DATE_SUB(NOW(), INTERVAL 11 MINUTE)),
    (41, 1, 1, NULL, 'TEXT', '감사합니다 도움이 될 것 같아요',                  DATE_SUB(NOW(), INTERVAL 10 MINUTE)),
    (42, 1, 2, NULL, 'TEXT', '다음주 같은 시간에 봐요',                         DATE_SUB(NOW(), INTERVAL  9 MINUTE)),
    (43, 1, 1, NULL, 'TEXT', '네 잊지 않겠습니다',                              DATE_SUB(NOW(), INTERVAL  8 MINUTE)),
    (44, 1, 2, NULL, 'TEXT', '면접 준비도 같이 해봐요',                         DATE_SUB(NOW(), INTERVAL  7 MINUTE)),
    (45, 1, 1, NULL, 'TEXT', '모의면접 해볼까요?',                              DATE_SUB(NOW(), INTERVAL  6 MINUTE)),
    (46, 1, 2, NULL, 'TEXT', '좋아요 제가 면접관 해볼게요',                     DATE_SUB(NOW(), INTERVAL  5 MINUTE)),
    (47, 1, 1, NULL, 'TEXT', '그럼 저는 지원자로 해볼게요',                     DATE_SUB(NOW(), INTERVAL  4 MINUTE)),
    (48, 1, 2, NULL, 'TEXT', '자기소개 부탁드립니다',                           DATE_SUB(NOW(), INTERVAL  3 MINUTE)),
    (49, 1, 1, NULL, 'TEXT', '안녕하세요 저는 백엔드 개발자입니다',             DATE_SUB(NOW(), INTERVAL  2 MINUTE)),
    (50, 1, 2, NULL, 'TEXT', '수고하셨습니다',                                  DATE_SUB(NOW(), INTERVAL  1 MINUTE));
