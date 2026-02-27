-- =====================================================================
-- [로컬 개발 전용] N+1 성능 테스트용 추가 시드 데이터
-- 목적: getMyChatRooms API의 N+1 문제를 수치로 확인하기 위해
--       userId=1이 참여 중인 채팅방을 20개로 늘림
-- 주의: 이 파일은 application-local.yml의 testdata 경로에서만 로딩됨
-- =====================================================================

-- ① 회사 추가 (company_id: 2~11, 공고 2개씩)
INSERT INTO companies (company_id, name, domain, created_at, updated_at) VALUES
                                                                             (2,  '네이버주식회사',   'naver.com',    NOW(), NOW()),
                                                                             (3,  '카카오주식회사',   'kakao.com',    NOW(), NOW()),
                                                                             (4,  '라인플러스',       'linecorp.com', NOW(), NOW()),
                                                                             (5,  '쿠팡',             'coupang.com',  NOW(), NOW()),
                                                                             (6,  '토스',             'toss.im',      NOW(), NOW()),
                                                                             (7,  '당근마켓',         'daangn.com',   NOW(), NOW()),
                                                                             (8,  '배달의민족',       'baemin.com',   NOW(), NOW()),
                                                                             (9,  '뱅크샐러드',       'banksalad.com',NOW(), NOW()),
                                                                             (10, '하이퍼커넥트',     'hyperconnect.com', NOW(), NOW()),
                                                                             (11, '직방',             'zigbang.com',  NOW(), NOW());

-- ② 채용공고 20개 (job_master_id: 2~21)
INSERT INTO job_masters (job_master_id, company_id, job_title, status, created_at, updated_at) VALUES
                                                                                                   (2,  2,  '백엔드 개발자 (Java/Spring)',   'OPEN', NOW(), NOW()),
                                                                                                   (3,  2,  '프론트엔드 개발자 (React)',     'OPEN', NOW(), NOW()),
                                                                                                   (4,  3,  '백엔드 개발자 (Kotlin)',        'OPEN', NOW(), NOW()),
                                                                                                   (5,  3,  'DevOps 엔지니어',               'OPEN', NOW(), NOW()),
                                                                                                   (6,  4,  '서버 개발자 (Go)',              'OPEN', NOW(), NOW()),
                                                                                                   (7,  4,  '백엔드 개발자 (Node.js)',       'OPEN', NOW(), NOW()),
                                                                                                   (8,  5,  '물류 플랫폼 백엔드',           'OPEN', NOW(), NOW()),
                                                                                                   (9,  5,  'ML 엔지니어',                  'OPEN', NOW(), NOW()),
                                                                                                   (10, 6,  '핀테크 백엔드 개발자',         'OPEN', NOW(), NOW()),
                                                                                                   (11, 6,  'iOS 개발자',                   'OPEN', NOW(), NOW()),
                                                                                                   (12, 7,  '중고거래 플랫폼 백엔드',       'OPEN', NOW(), NOW()),
                                                                                                   (13, 7,  '안드로이드 개발자',            'OPEN', NOW(), NOW()),
                                                                                                   (14, 8,  '주문 시스템 백엔드',           'OPEN', NOW(), NOW()),
                                                                                                   (15, 8,  '데이터 엔지니어',              'OPEN', NOW(), NOW()),
                                                                                                   (16, 9,  '오픈뱅킹 백엔드',             'OPEN', NOW(), NOW()),
                                                                                                   (17, 9,  'QA 엔지니어',                 'OPEN', NOW(), NOW()),
                                                                                                   (18, 10, '영상처리 백엔드',             'OPEN', NOW(), NOW()),
                                                                                                   (19, 10,'보안 엔지니어',               'OPEN', NOW(), NOW()),
                                                                                                   (20, 11, '부동산 플랫폼 백엔드',        'OPEN', NOW(), NOW()),
                                                                                                   (21, 11, '풀스택 개발자',               'OPEN', NOW(), NOW());

-- ③ job_post 20개 (각 job_master에 1개씩)
INSERT INTO job_posts (job_post_id, job_master_id, ai_job_id, company_id, source_type, source_url, source_url_hash,
                       raw_company_name, raw_job_title, recruitment_status, registration_status, fingerprint_hash, created_at, updated_at)
VALUES
    (2,  2,  2,  2,  'MANUAL', 'https://naver.com/job/1',  SHA2('https://naver.com/job/1',  256), '네이버',   '백엔드 개발자',   'OPEN', 'REGISTERED', SHA2('fp-2',  256), NOW(), NOW()),
    (3,  3,  3,  2,  'MANUAL', 'https://naver.com/job/2',  SHA2('https://naver.com/job/2',  256), '네이버',   '프론트 개발자',   'OPEN', 'REGISTERED', SHA2('fp-3',  256), NOW(), NOW()),
    (4,  4,  4,  3,  'MANUAL', 'https://kakao.com/job/1',  SHA2('https://kakao.com/job/1',  256), '카카오',   '백엔드 개발자',   'OPEN', 'REGISTERED', SHA2('fp-4',  256), NOW(), NOW()),
    (5,  5,  5,  3,  'MANUAL', 'https://kakao.com/job/2',  SHA2('https://kakao.com/job/2',  256), '카카오',   'DevOps',          'OPEN', 'REGISTERED', SHA2('fp-5',  256), NOW(), NOW()),
    (6,  6,  6,  4,  'MANUAL', 'https://line.com/job/1',   SHA2('https://line.com/job/1',   256), '라인',     '서버 개발자',     'OPEN', 'REGISTERED', SHA2('fp-6',  256), NOW(), NOW()),
    (7,  7,  7,  4,  'MANUAL', 'https://line.com/job/2',   SHA2('https://line.com/job/2',   256), '라인',     '백엔드 개발자',   'OPEN', 'REGISTERED', SHA2('fp-7',  256), NOW(), NOW()),
    (8,  8,  8,  5,  'MANUAL', 'https://coupang.com/job/1',SHA2('https://coupang.com/job/1',256), '쿠팡',     '물류 백엔드',     'OPEN', 'REGISTERED', SHA2('fp-8',  256), NOW(), NOW()),
    (9,  9,  9,  5,  'MANUAL', 'https://coupang.com/job/2',SHA2('https://coupang.com/job/2',256), '쿠팡',     'ML 엔지니어',     'OPEN', 'REGISTERED', SHA2('fp-9',  256), NOW(), NOW()),
    (10, 10, 10, 6,  'MANUAL', 'https://toss.im/job/1',    SHA2('https://toss.im/job/1',    256), '토스',     '핀테크 백엔드',   'OPEN', 'REGISTERED', SHA2('fp-10', 256), NOW(), NOW()),
    (11, 11, 11, 6,  'MANUAL', 'https://toss.im/job/2',    SHA2('https://toss.im/job/2',    256), '토스',     'iOS 개발자',      'OPEN', 'REGISTERED', SHA2('fp-11', 256), NOW(), NOW()),
    (12, 12, 12, 7,  'MANUAL', 'https://daangn.com/job/1', SHA2('https://daangn.com/job/1', 256), '당근',     '중고거래 백엔드', 'OPEN', 'REGISTERED', SHA2('fp-12', 256), NOW(), NOW()),
    (13, 13, 13, 7,  'MANUAL', 'https://daangn.com/job/2', SHA2('https://daangn.com/job/2', 256), '당근',     '안드로이드',      'OPEN', 'REGISTERED', SHA2('fp-13', 256), NOW(), NOW()),
    (14, 14, 14, 8,  'MANUAL', 'https://baemin.com/job/1', SHA2('https://baemin.com/job/1', 256), '배민',     '주문 백엔드',     'OPEN', 'REGISTERED', SHA2('fp-14', 256), NOW(), NOW()),
    (15, 15, 15, 8,  'MANUAL', 'https://baemin.com/job/2', SHA2('https://baemin.com/job/2', 256), '배민',     '데이터 엔지니어', 'OPEN', 'REGISTERED', SHA2('fp-15', 256), NOW(), NOW()),
    (16, 16, 16, 9,  'MANUAL', 'https://banksalad.com/job/1',SHA2('https://banksalad.com/job/1',256),'뱅크샐러드','오픈뱅킹 백엔드','OPEN','REGISTERED',SHA2('fp-16',256),NOW(),NOW()),
    (17, 17, 17, 9,  'MANUAL', 'https://banksalad.com/job/2',SHA2('https://banksalad.com/job/2',256),'뱅크샐러드','QA 엔지니어',   'OPEN','REGISTERED',SHA2('fp-17',256),NOW(),NOW()),
    (18, 18, 18, 10, 'MANUAL', 'https://hyperconnect.com/job/1',SHA2('https://hyperconnect.com/job/1',256),'하이퍼커넥트','영상처리 백엔드','OPEN','REGISTERED',SHA2('fp-18',256),NOW(),NOW()),
    (19, 19, 19, 10, 'MANUAL', 'https://hyperconnect.com/job/2',SHA2('https://hyperconnect.com/job/2',256),'하이퍼커넥트','보안 엔지니어','OPEN','REGISTERED',SHA2('fp-19',256),NOW(),NOW()),
    (20, 20, 20, 11, 'MANUAL', 'https://zigbang.com/job/1',SHA2('https://zigbang.com/job/1',256),'직방','부동산 백엔드','OPEN','REGISTERED',SHA2('fp-20',256),NOW(),NOW()),
    (21, 21, 21, 11, 'MANUAL', 'https://zigbang.com/job/2',SHA2('https://zigbang.com/job/2',256),'직방','풀스택 개발자','OPEN','REGISTERED',SHA2('fp-21',256),NOW(),NOW());

-- ④ userId=1 지원서 20개 (job_application_id: 3~22)
INSERT INTO job_applications (job_application_id, user_id, job_master_id, status, created_at, updated_at) VALUES
                                                                                                              (3,  1, 2,  'SUBMITTED', NOW(), NOW()),
                                                                                                              (4,  1, 3,  'SUBMITTED', NOW(), NOW()),
                                                                                                              (5,  1, 4,  'SUBMITTED', NOW(), NOW()),
                                                                                                              (6,  1, 5,  'SUBMITTED', NOW(), NOW()),
                                                                                                              (7,  1, 6,  'SUBMITTED', NOW(), NOW()),
                                                                                                              (8,  1, 7,  'SUBMITTED', NOW(), NOW()),
                                                                                                              (9,  1, 8,  'SUBMITTED', NOW(), NOW()),
                                                                                                              (10, 1, 9,  'SUBMITTED', NOW(), NOW()),
                                                                                                              (11, 1, 10, 'SUBMITTED', NOW(), NOW()),
                                                                                                              (12, 1, 11, 'SUBMITTED', NOW(), NOW()),
                                                                                                              (13, 1, 12, 'SUBMITTED', NOW(), NOW()),
                                                                                                              (14, 1, 13, 'SUBMITTED', NOW(), NOW()),
                                                                                                              (15, 1, 14, 'SUBMITTED', NOW(), NOW()),
                                                                                                              (16, 1, 15, 'SUBMITTED', NOW(), NOW()),
                                                                                                              (17, 1, 16, 'SUBMITTED', NOW(), NOW()),
                                                                                                              (18, 1, 17, 'SUBMITTED', NOW(), NOW()),
                                                                                                              (19, 1, 18, 'SUBMITTED', NOW(), NOW()),
                                                                                                              (20, 1, 19, 'SUBMITTED', NOW(), NOW()),
                                                                                                              (21, 1, 20, 'SUBMITTED', NOW(), NOW()),
                                                                                                              (22, 1, 21, 'SUBMITTED', NOW(), NOW());

-- ⑤ AI 평가 20개 (evaluation_id: 3~22)
INSERT INTO ai_applicant_evaluation (evaluation_id, job_application_id, overall_score,
                                     one_line_review, feedback_detail, comparison_scores, created_at, updated_at) VALUES
                                                                                                                      (3,  3,  88, '우수한 개발자', '훌륭합니다', '[]', NOW(), NOW()),
                                                                                                                      (4,  4,  82, '우수한 개발자', '훌륭합니다', '[]', NOW(), NOW()),
                                                                                                                      (5,  5,  91, '우수한 개발자', '훌륭합니다', '[]', NOW(), NOW()),
                                                                                                                      (6,  6,  85, '우수한 개발자', '훌륭합니다', '[]', NOW(), NOW()),
                                                                                                                      (7,  7,  87, '우수한 개발자', '훌륭합니다', '[]', NOW(), NOW()),
                                                                                                                      (8,  8,  83, '우수한 개발자', '훌륭합니다', '[]', NOW(), NOW()),
                                                                                                                      (9,  9,  90, '우수한 개발자', '훌륭합니다', '[]', NOW(), NOW()),
                                                                                                                      (10, 10, 86, '우수한 개발자', '훌륭합니다', '[]', NOW(), NOW()),
                                                                                                                      (11, 11, 89, '우수한 개발자', '훌륭합니다', '[]', NOW(), NOW()),
                                                                                                                      (12, 12, 84, '우수한 개발자', '훌륭합니다', '[]', NOW(), NOW()),
                                                                                                                      (13, 13, 92, '우수한 개발자', '훌륭합니다', '[]', NOW(), NOW()),
                                                                                                                      (14, 14, 81, '우수한 개발자', '훌륭합니다', '[]', NOW(), NOW()),
                                                                                                                      (15, 15, 93, '우수한 개발자', '훌륭합니다', '[]', NOW(), NOW()),
                                                                                                                      (16, 16, 80, '우수한 개발자', '훌륭합니다', '[]', NOW(), NOW()),
                                                                                                                      (17, 17, 88, '우수한 개발자', '훌륭합니다', '[]', NOW(), NOW()),
                                                                                                                      (18, 18, 85, '우수한 개발자', '훌륭합니다', '[]', NOW(), NOW()),
                                                                                                                      (19, 19, 87, '우수한 개발자', '훌륭합니다', '[]', NOW(), NOW()),
                                                                                                                      (20, 20, 83, '우수한 개발자', '훌륭합니다', '[]', NOW(), NOW()),
                                                                                                                      (21, 21, 90, '우수한 개발자', '훌륭합니다', '[]', NOW(), NOW()),
                                                                                                                      (22, 22, 86, '우수한 개발자', '훌륭합니다', '[]', NOW(), NOW());

-- ⑥ 채팅방 20개 (chat_room_id: 2~21, userId=1이 방장)
INSERT INTO chat_rooms (chat_room_id, job_master_id, created_by, room_name, max_participants,
                        room_goal, cutline_score, preferred_conditions, status, created_at, updated_at) VALUES
                                                                                                            (2,  2,  1, '네이버 백엔드 스터디',     8, 'INTERVIEW', 80, NULL, 'ACTIVE', NOW(), NOW()),
                                                                                                            (3,  3,  1, '네이버 프론트 스터디',     8, 'INTERVIEW', 75, NULL, 'ACTIVE', NOW(), NOW()),
                                                                                                            (4,  4,  1, '카카오 백엔드 스터디',     8, 'INTERVIEW', 80, NULL, 'ACTIVE', NOW(), NOW()),
                                                                                                            (5,  5,  1, '카카오 DevOps 스터디',     8, 'INTERVIEW', 75, NULL, 'ACTIVE', NOW(), NOW()),
                                                                                                            (6,  6,  1, '라인 서버 스터디',         8, 'INTERVIEW', 80, NULL, 'ACTIVE', NOW(), NOW()),
                                                                                                            (7,  7,  1, '라인 백엔드 스터디',       8, 'INTERVIEW', 75, NULL, 'ACTIVE', NOW(), NOW()),
                                                                                                            (8,  8,  1, '쿠팡 물류 스터디',         8, 'INTERVIEW', 80, NULL, 'ACTIVE', NOW(), NOW()),
                                                                                                            (9,  9,  1, '쿠팡 ML 스터디',           8, 'INTERVIEW', 75, NULL, 'ACTIVE', NOW(), NOW()),
                                                                                                            (10, 10, 1, '토스 핀테크 스터디',       8, 'INTERVIEW', 80, NULL, 'ACTIVE', NOW(), NOW()),
                                                                                                            (11, 11, 1, '토스 iOS 스터디',          8, 'INTERVIEW', 75, NULL, 'ACTIVE', NOW(), NOW()),
                                                                                                            (12, 12, 1, '당근 백엔드 스터디',       8, 'INTERVIEW', 80, NULL, 'ACTIVE', NOW(), NOW()),
                                                                                                            (13, 13, 1, '당근 안드로이드 스터디',   8, 'INTERVIEW', 75, NULL, 'ACTIVE', NOW(), NOW()),
                                                                                                            (14, 14, 1, '배민 주문 스터디',         8, 'INTERVIEW', 80, NULL, 'ACTIVE', NOW(), NOW()),
                                                                                                            (15, 15, 1, '배민 데이터 스터디',       8, 'INTERVIEW', 75, NULL, 'ACTIVE', NOW(), NOW()),
                                                                                                            (16, 16, 1, '뱅크샐러드 백엔드 스터디', 8, 'INTERVIEW', 80, NULL, 'ACTIVE', NOW(), NOW()),
                                                                                                            (17, 17, 1, '뱅크샐러드 QA 스터디',    8, 'INTERVIEW', 75, NULL, 'ACTIVE', NOW(), NOW()),
                                                                                                            (18, 18, 1, '하이퍼커넥트 스터디',     8, 'INTERVIEW', 80, NULL, 'ACTIVE', NOW(), NOW()),
                                                                                                            (19, 19, 1, '하이퍼커넥트 보안 스터디', 8, 'INTERVIEW', 75, NULL, 'ACTIVE', NOW(), NOW()),
                                                                                                            (20, 20, 1, '직방 백엔드 스터디',       8, 'INTERVIEW', 80, NULL, 'ACTIVE', NOW(), NOW()),
                                                                                                            (21, 21, 1, '직방 풀스택 스터디',       8, 'INTERVIEW', 75, NULL, 'ACTIVE', NOW(), NOW());

-- ⑦ chat_room_members 20개 (userId=1, HOST)
INSERT INTO chat_room_members (chat_room_member_id, chat_room_id, user_id, job_application_id, role, joined_at) VALUES
                                                                                                                    (3,  2,  1, 3,  'HOST', NOW()),
                                                                                                                    (4,  3,  1, 4,  'HOST', NOW()),
                                                                                                                    (5,  4,  1, 5,  'HOST', NOW()),
                                                                                                                    (6,  5,  1, 6,  'HOST', NOW()),
                                                                                                                    (7,  6,  1, 7,  'HOST', NOW()),
                                                                                                                    (8,  7,  1, 8,  'HOST', NOW()),
                                                                                                                    (9,  8,  1, 9,  'HOST', NOW()),
                                                                                                                    (10, 9,  1, 10, 'HOST', NOW()),
                                                                                                                    (11, 10, 1, 11, 'HOST', NOW()),
                                                                                                                    (12, 11, 1, 12, 'HOST', NOW()),
                                                                                                                    (13, 12, 1, 13, 'HOST', NOW()),
                                                                                                                    (14, 13, 1, 14, 'HOST', NOW()),
                                                                                                                    (15, 14, 1, 15, 'HOST', NOW()),
                                                                                                                    (16, 15, 1, 16, 'HOST', NOW()),
                                                                                                                    (17, 16, 1, 17, 'HOST', NOW()),
                                                                                                                    (18, 17, 1, 18, 'HOST', NOW()),
                                                                                                                    (19, 18, 1, 19, 'HOST', NOW()),
                                                                                                                    (20, 19, 1, 20, 'HOST', NOW()),
                                                                                                                    (21, 20, 1, 21, 'HOST', NOW()),
                                                                                                                    (22, 21, 1, 22, 'HOST', NOW());