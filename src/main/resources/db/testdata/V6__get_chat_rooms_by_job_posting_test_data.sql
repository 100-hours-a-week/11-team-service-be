-- =====================================================================
-- [로컬 개발 전용] getChatRoomsByJobPosting 8N+1 성능 테스트용 시드 데이터
-- 목적: job_master_id=1 하나에 채팅방 10개를 만들어
--       채팅방 수에 비례한 8N+1 쿼리 문제를 수치로 확인하기 위함
-- 구조:
--   - 유저 3~12: 각각 job_master_id=1에 채팅방 1개씩 생성 (방장)
--   - userId=1: job_master_id=1에 지원서+이력서+AI평가 보유 (조회 주체)
--   - 조회 시 채팅방 1개당 8번 쿼리 반복 → 총 4 + (8 × 10) = 84번
-- 주의: 이 파일은 application-local.yml의 testdata 경로에서만 로딩됨
-- =====================================================================

-- ① 더미 파일 오브젝트 (이력서용, file_id=1)
--    application_documents.file_id가 NOT NULL이므로 더미 레코드 필요
INSERT INTO file_objects (file_id, storage_provider, bucket, object_key, original_name,
                          content_type, size_bytes, checksum, created_at, deleted_at)
VALUES (1, 'LOCAL', 'test-bucket', 'resumes/dummy-resume.pdf', 'dummy-resume.pdf',
        'application/pdf', 1024, NULL, NOW(), NULL);

-- ② 유저 3~12 생성 (각 채팅방 방장)
INSERT INTO users (user_id, profile_image_file_id, role, nickname, status, created_at, updated_at) VALUES
    ( 3, NULL, 'USER', 'room-host-03', 'ACTIVE', NOW(), NOW()),
    ( 4, NULL, 'USER', 'room-host-04', 'ACTIVE', NOW(), NOW()),
    ( 5, NULL, 'USER', 'room-host-05', 'ACTIVE', NOW(), NOW()),
    ( 6, NULL, 'USER', 'room-host-06', 'ACTIVE', NOW(), NOW()),
    ( 7, NULL, 'USER', 'room-host-07', 'ACTIVE', NOW(), NOW()),
    ( 8, NULL, 'USER', 'room-host-08', 'ACTIVE', NOW(), NOW()),
    ( 9, NULL, 'USER', 'room-host-09', 'ACTIVE', NOW(), NOW()),
    (10, NULL, 'USER', 'room-host-10', 'ACTIVE', NOW(), NOW()),
    (11, NULL, 'USER', 'room-host-11', 'ACTIVE', NOW(), NOW()),
    (12, NULL, 'USER', 'room-host-12', 'ACTIVE', NOW(), NOW());

-- ③ OAuth 계정 (유저 3~12)
INSERT INTO user_oauth_accounts (user_id, email, provider, provider_user_id, provider_email, connected_at, created_at, updated_at) VALUES
    ( 3, 'host03@test.com', 'KAKAO', 'kakao_host_03', 'host03@test.com', NOW(), NOW(), NOW()),
    ( 4, 'host04@test.com', 'KAKAO', 'kakao_host_04', 'host04@test.com', NOW(), NOW(), NOW()),
    ( 5, 'host05@test.com', 'KAKAO', 'kakao_host_05', 'host05@test.com', NOW(), NOW(), NOW()),
    ( 6, 'host06@test.com', 'KAKAO', 'kakao_host_06', 'host06@test.com', NOW(), NOW(), NOW()),
    ( 7, 'host07@test.com', 'KAKAO', 'kakao_host_07', 'host07@test.com', NOW(), NOW(), NOW()),
    ( 8, 'host08@test.com', 'KAKAO', 'kakao_host_08', 'host08@test.com', NOW(), NOW(), NOW()),
    ( 9, 'host09@test.com', 'KAKAO', 'kakao_host_09', 'host09@test.com', NOW(), NOW(), NOW()),
    (10, 'host10@test.com', 'KAKAO', 'kakao_host_10', 'host10@test.com', NOW(), NOW(), NOW()),
    (11, 'host11@test.com', 'KAKAO', 'kakao_host_11', 'host11@test.com', NOW(), NOW(), NOW()),
    (12, 'host12@test.com', 'KAKAO', 'kakao_host_12', 'host12@test.com', NOW(), NOW(), NOW());

-- ④ 지원서: 유저 3~12가 job_master_id=1에 지원 (job_application_id: 23~32)
INSERT INTO job_applications (job_application_id, user_id, job_master_id, status, created_at, updated_at) VALUES
    (23,  3, 1, 'SUBMITTED', NOW(), NOW()),
    (24,  4, 1, 'SUBMITTED', NOW(), NOW()),
    (25,  5, 1, 'SUBMITTED', NOW(), NOW()),
    (26,  6, 1, 'SUBMITTED', NOW(), NOW()),
    (27,  7, 1, 'SUBMITTED', NOW(), NOW()),
    (28,  8, 1, 'SUBMITTED', NOW(), NOW()),
    (29,  9, 1, 'SUBMITTED', NOW(), NOW()),
    (30, 10, 1, 'SUBMITTED', NOW(), NOW()),
    (31, 11, 1, 'SUBMITTED', NOW(), NOW()),
    (32, 12, 1, 'SUBMITTED', NOW(), NOW());

-- ⑤ AI 평가: 유저 3~12 (evaluation_id: 23~32, score >= 80으로 cutline 통과 가능)
INSERT INTO ai_applicant_evaluation (evaluation_id, job_application_id, overall_score,
                                     one_line_review, feedback_detail, comparison_scores, created_at, updated_at) VALUES
    (23, 23, 85, '우수한 개발자', '훌륭합니다', '[]', NOW(), NOW()),
    (24, 24, 88, '우수한 개발자', '훌륭합니다', '[]', NOW(), NOW()),
    (25, 25, 82, '우수한 개발자', '훌륭합니다', '[]', NOW(), NOW()),
    (26, 26, 91, '우수한 개발자', '훌륭합니다', '[]', NOW(), NOW()),
    (27, 27, 87, '우수한 개발자', '훌륭합니다', '[]', NOW(), NOW()),
    (28, 28, 83, '우수한 개발자', '훌륭합니다', '[]', NOW(), NOW()),
    (29, 29, 90, '우수한 개발자', '훌륭합니다', '[]', NOW(), NOW()),
    (30, 30, 86, '우수한 개발자', '훌륭합니다', '[]', NOW(), NOW()),
    (31, 31, 89, '우수한 개발자', '훌륭합니다', '[]', NOW(), NOW()),
    (32, 32, 84, '우수한 개발자', '훌륭합니다', '[]', NOW(), NOW());

-- ⑥ 이력서 서류: 유저 3~12 (application_document_id: 2~11)
--    determineJoinEligibility의 이력서 확인 쿼리(existsByJobApplicationIdAndDocType)가
--    RESUME를 찾도록 하기 위함
INSERT INTO application_documents (application_document_id, job_application_id, file_id, doc_type, created_at, updated_at) VALUES
    ( 2, 23, 1, 'RESUME', NOW(), NOW()),
    ( 3, 24, 1, 'RESUME', NOW(), NOW()),
    ( 4, 25, 1, 'RESUME', NOW(), NOW()),
    ( 5, 26, 1, 'RESUME', NOW(), NOW()),
    ( 6, 27, 1, 'RESUME', NOW(), NOW()),
    ( 7, 28, 1, 'RESUME', NOW(), NOW()),
    ( 8, 29, 1, 'RESUME', NOW(), NOW()),
    ( 9, 30, 1, 'RESUME', NOW(), NOW()),
    (10, 31, 1, 'RESUME', NOW(), NOW()),
    (11, 32, 1, 'RESUME', NOW(), NOW());

-- ⑦ 이력서 서류: userId=1 (job_application_id=1)
--    userId=1이 조회 주체일 때 determineJoinEligibility가
--    이력서 확인 쿼리까지 실행되도록 하기 위함
INSERT INTO application_documents (application_document_id, job_application_id, file_id, doc_type, created_at, updated_at) VALUES
    (1, 1, 1, 'RESUME', NOW(), NOW());

-- ⑧ 채팅방 10개 (chat_room_id: 22~31, job_master_id=1, 방장: 유저 3~12)
--    cutline_score=80으로 설정 → userId=1의 AI 점수(90)가 통과되어
--    determineJoinEligibility의 모든 8개 쿼리가 실행됨
INSERT INTO chat_rooms (chat_room_id, job_master_id, created_by, room_name, max_participants,
                        room_goal, cutline_score, preferred_conditions, status, created_at, updated_at) VALUES
    (22, 1,  3, '백엔드 스터디방 #2',  8, 'INTERVIEW', 80, NULL, 'ACTIVE', NOW(), NOW()),
    (23, 1,  4, '백엔드 스터디방 #3',  8, 'INTERVIEW', 80, NULL, 'ACTIVE', NOW(), NOW()),
    (24, 1,  5, '백엔드 스터디방 #4',  8, 'INTERVIEW', 80, NULL, 'ACTIVE', NOW(), NOW()),
    (25, 1,  6, '백엔드 스터디방 #5',  8, 'INTERVIEW', 80, NULL, 'ACTIVE', NOW(), NOW()),
    (26, 1,  7, '백엔드 스터디방 #6',  8, 'INTERVIEW', 80, NULL, 'ACTIVE', NOW(), NOW()),
    (27, 1,  8, '백엔드 스터디방 #7',  8, 'INTERVIEW', 80, NULL, 'ACTIVE', NOW(), NOW()),
    (28, 1,  9, '백엔드 스터디방 #8',  8, 'INTERVIEW', 80, NULL, 'ACTIVE', NOW(), NOW()),
    (29, 1, 10, '백엔드 스터디방 #9',  8, 'INTERVIEW', 80, NULL, 'ACTIVE', NOW(), NOW()),
    (30, 1, 11, '백엔드 스터디방 #10', 8, 'INTERVIEW', 80, NULL, 'ACTIVE', NOW(), NOW()),
    (31, 1, 12, '백엔드 스터디방 #11', 8, 'INTERVIEW', 80, NULL, 'ACTIVE', NOW(), NOW());

-- ⑨ 채팅방 멤버: 유저 3~12가 각 방의 HOST로 등록
INSERT INTO chat_room_members (chat_room_member_id, chat_room_id, user_id, job_application_id, role, joined_at) VALUES
    (23, 22,  3, 23, 'HOST', NOW()),
    (24, 23,  4, 24, 'HOST', NOW()),
    (25, 24,  5, 25, 'HOST', NOW()),
    (26, 25,  6, 26, 'HOST', NOW()),
    (27, 26,  7, 27, 'HOST', NOW()),
    (28, 27,  8, 28, 'HOST', NOW()),
    (29, 28,  9, 29, 'HOST', NOW()),
    (30, 29, 10, 30, 'HOST', NOW()),
    (31, 30, 11, 31, 'HOST', NOW()),
    (32, 31, 12, 32, 'HOST', NOW());
