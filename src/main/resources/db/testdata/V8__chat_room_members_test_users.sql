-- =====================================================================
-- [로컬 개발 전용] getChatRoomMembers N+1 테스트용 유저 시드
-- 목적: GET /api/v1/chat-rooms/{chatRoomId}/members 의 N+1 개선 전/후 성능 비교
--
-- 의존성: V2 (user_id=1,2 존재 필수)
-- 설계 의도: V9에서 chat_room_id=1에 멤버로 추가할 userId=3,4,5 생성
--            V9이 이 파일에 FK 의존하므로 반드시 V8 → V9 순서로 적재
-- 버전 주의: migration/V5 가 DDL로 사용 중이므로 V8 사용
-- =====================================================================

-- ① 유저 3~12 생성 (room-member-03 ~ room-member-12)
-- V9에서 chat_room_id=1의 MEMBER로 추가할 userId=3,4,5 포함
INSERT INTO users (user_id, profile_image_file_id, role, nickname, status, created_at, updated_at)
VALUES
    ( 3, NULL, 'USER', 'room-member-03', 'ACTIVE', NOW(), NOW()),
    ( 4, NULL, 'USER', 'room-member-04', 'ACTIVE', NOW(), NOW()),
    ( 5, NULL, 'USER', 'room-member-05', 'ACTIVE', NOW(), NOW()),
    ( 6, NULL, 'USER', 'room-member-06', 'ACTIVE', NOW(), NOW()),
    ( 7, NULL, 'USER', 'room-member-07', 'ACTIVE', NOW(), NOW()),
    ( 8, NULL, 'USER', 'room-member-08', 'ACTIVE', NOW(), NOW()),
    ( 9, NULL, 'USER', 'room-member-09', 'ACTIVE', NOW(), NOW()),
    (10, NULL, 'USER', 'room-member-10', 'ACTIVE', NOW(), NOW()),
    (11, NULL, 'USER', 'room-member-11', 'ACTIVE', NOW(), NOW()),
    (12, NULL, 'USER', 'room-member-12', 'ACTIVE', NOW(), NOW());

-- ② OAuth 계정 연결 (유저 3~12)
INSERT INTO user_oauth_accounts (user_id, email, provider, provider_user_id, provider_email, connected_at, created_at, updated_at)
VALUES
    ( 3, 'member03@test.com', 'KAKAO', 'kakao_test_3',  'member03@test.com', NOW(), NOW(), NOW()),
    ( 4, 'member04@test.com', 'KAKAO', 'kakao_test_4',  'member04@test.com', NOW(), NOW(), NOW()),
    ( 5, 'member05@test.com', 'KAKAO', 'kakao_test_5',  'member05@test.com', NOW(), NOW(), NOW()),
    ( 6, 'member06@test.com', 'KAKAO', 'kakao_test_6',  'member06@test.com', NOW(), NOW(), NOW()),
    ( 7, 'member07@test.com', 'KAKAO', 'kakao_test_7',  'member07@test.com', NOW(), NOW(), NOW()),
    ( 8, 'member08@test.com', 'KAKAO', 'kakao_test_8',  'member08@test.com', NOW(), NOW(), NOW()),
    ( 9, 'member09@test.com', 'KAKAO', 'kakao_test_9',  'member09@test.com', NOW(), NOW(), NOW()),
    (10, 'member10@test.com', 'KAKAO', 'kakao_test_10', 'member10@test.com', NOW(), NOW(), NOW()),
    (11, 'member11@test.com', 'KAKAO', 'kakao_test_11', 'member11@test.com', NOW(), NOW(), NOW()),
    (12, 'member12@test.com', 'KAKAO', 'kakao_test_12', 'member12@test.com', NOW(), NOW(), NOW());

-- ③ 유저 3~5의 job_master_id=1 지원서 생성 (V9에서 chat_room_members FK 필요)
-- V2에서 job_application_id=1(userId=1), 2(userId=2) 사용
INSERT INTO job_applications (job_application_id, user_id, job_master_id, status, created_at, updated_at)
VALUES
    (3, 3, 1, 'SUBMITTED', NOW(), NOW()),
    (4, 4, 1, 'SUBMITTED', NOW(), NOW()),
    (5, 5, 1, 'SUBMITTED', NOW(), NOW());

-- ④ 유저 3~5의 AI 평가 생성 (chat_room 입장 조건 충족용, score >= cutline_score=80)
-- V2에서 evaluation_id=1,2 사용
INSERT INTO ai_applicant_evaluation (evaluation_id, job_application_id, overall_score,
                                     one_line_review, feedback_detail, comparison_scores, created_at, updated_at)
VALUES
    (3, 3, 82, '역량 있는 개발자', '기술력이 검증됐습니다', '[]', NOW(), NOW()),
    (4, 4, 83, '우수한 개발자',   '코드 품질이 높습니다',   '[]', NOW(), NOW()),
    (5, 5, 84, '탁월한 개발자',   '문제 해결 능력 우수',    '[]', NOW(), NOW());
