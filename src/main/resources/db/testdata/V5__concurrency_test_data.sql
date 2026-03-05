-- =====================================================================
-- [로컬 개발 전용] 동시성 문제 관측용 시드 데이터
-- 목적:
--   이슈6 - 채팅방 동시 입장 정원 초과 race condition 재현
--   이슈8 - AI 비교 분석 동시 요청 중복 저장 race condition 재현
--
-- 의존성: V2__test_seed_data.sql 이후 실행 (userId=1,2 / chatRoomId=1 이미 존재)
--
-- 이슈6 시나리오:
--   chat_room_id=2, max_participants=2, userId=3이 HOST (1명)
--   → userId=4, userId=5가 동시에 입장 시도 → 정원 초과 race condition 발생
--
-- 이슈8 시나리오:
--   V2에서 이미 userId=1(HOST), userId=2(MEMBER)가 chat_room_id=1에 존재
--   → userId=1이 chatRoomMemberId=2와의 비교 분석을 동시 2회 요청
--   → DB 중복 레코드 저장 가능성 확인 (unique constraint 없음)
-- =====================================================================

-- ① 더미 파일 오브젝트 (이력서 FK 용도)
--    application_documents.file_id NOT NULL 이므로 더미 레코드 필요
INSERT INTO file_objects (file_id, storage_provider, bucket, object_key, original_name,
                          content_type, size_bytes, checksum, created_at, deleted_at)
VALUES (1, 'LOCAL', 'test-bucket', 'resumes/dummy-resume.pdf', 'dummy-resume.pdf',
        'application/pdf', 1024, NULL, NOW(), NULL);

-- ② 유저 3, 4, 5 생성
--    userId=3 : chat_room_id=2의 방장
--    userId=4,5 : 동시에 chat_room_id=2 입장을 시도할 race condition 대상
INSERT INTO users (user_id, profile_image_file_id, role, nickname, status, created_at, updated_at)
VALUES
    (3, NULL, 'USER', 'race-host',    'ACTIVE', NOW(), NOW()),
    (4, NULL, 'USER', 'race-joiner1', 'ACTIVE', NOW(), NOW()),
    (5, NULL, 'USER', 'race-joiner2', 'ACTIVE', NOW(), NOW());

-- ③ OAuth 계정 (userId=3,4,5)
INSERT INTO user_oauth_accounts (user_id, email, provider, provider_user_id, provider_email,
                                 connected_at, created_at, updated_at)
VALUES
    (3, 'host3@test.com',    'KAKAO', 'kakao_race_host',    'host3@test.com',    NOW(), NOW(), NOW()),
    (4, 'joiner4@test.com',  'KAKAO', 'kakao_race_joiner1', 'joiner4@test.com',  NOW(), NOW(), NOW()),
    (5, 'joiner5@test.com',  'KAKAO', 'kakao_race_joiner2', 'joiner5@test.com',  NOW(), NOW(), NOW());

-- ④ 지원서 (userId=3,4,5 → job_master_id=1)
--    joinChatRoom: findByUserIdAndJobMasterId 로 지원서 조회 필요
INSERT INTO job_applications (job_application_id, user_id, job_master_id, status, created_at, updated_at)
VALUES
    (3, 3, 1, 'SUBMITTED', NOW(), NOW()),
    (4, 4, 1, 'SUBMITTED', NOW(), NOW()),
    (5, 5, 1, 'SUBMITTED', NOW(), NOW());

-- ⑤ AI 평가 (job_application_id=3,4,5)
--    cutline_score=80 인 chat_room_id=2 입장 조건 통과용 (score >= 80)
INSERT INTO ai_applicant_evaluation (evaluation_id, job_application_id, overall_score,
                                     one_line_review, feedback_detail, comparison_scores,
                                     created_at, updated_at)
VALUES
    (3, 3, 87, '우수한 개발자', '훌륭합니다', '[]', NOW(), NOW()),
    (4, 4, 85, '우수한 개발자', '훌륭합니다', '[]', NOW(), NOW()),
    (5, 5, 83, '우수한 개발자', '훌륭합니다', '[]', NOW(), NOW());

-- ⑥ 이력서 서류 (job_application_id=1,2,3,4,5)
--    joinChatRoom: existsByJobApplicationIdAndDocType(RESUME) 검증 통과용
--    userId=1,2(job_app 1,2)는 이슈8 compare 요청 시 직접적으로는 불필요하지만
--    일관성을 위해 추가
INSERT INTO application_documents (application_document_id, job_application_id, file_id,
                                   doc_type, created_at, updated_at)
VALUES
    (1, 1, 1, 'RESUME', NOW(), NOW()),  -- userId=1
    (2, 2, 1, 'RESUME', NOW(), NOW()),  -- userId=2
    (3, 3, 1, 'RESUME', NOW(), NOW()),  -- userId=3 (race-host)
    (4, 4, 1, 'RESUME', NOW(), NOW()),  -- userId=4 (race-joiner1)
    (5, 5, 1, 'RESUME', NOW(), NOW());  -- userId=5 (race-joiner2)

-- ⑦ 채팅방 (chat_room_id=2) — 이슈6 race condition 대상
--    max_participants=2, 현재 userId=3(HOST) 1명만 입장한 상태
--    → userId=4, userId=5가 동시에 joinChatRoom 호출하면
--      두 요청 모두 count=1 < max=2 통과 후 저장 → 3명이 되어 정원 초과
INSERT INTO chat_rooms (chat_room_id, job_master_id, created_by, room_name, max_participants,
                        room_goal, cutline_score, preferred_conditions, status,
                        created_at, updated_at)
VALUES (2, 1, 3, 'race-condition 테스트방', 2, 'INTERVIEW', 80, NULL, 'ACTIVE', NOW(), NOW());

-- ⑧ 채팅방 멤버 (userId=3가 chat_room_id=2의 HOST)
INSERT INTO chat_room_members (chat_room_member_id, chat_room_id, user_id, job_application_id,
                                role, joined_at)
VALUES (3, 2, 3, 3, 'HOST', NOW());
