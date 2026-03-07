-- =====================================================================
-- [로컬 개발 전용] getMyChatRooms 4N+1 테스트용 시드 데이터
-- 목적: GET /api/v1/users/me/chat-rooms 의 N+1 개선 전/후 성능 비교
--
-- 의존성: V2 (userId=1, company_id=1, job_master_id=1 존재 필수)
-- 설계 의도: userId=1이 방장으로 총 21개 채팅방 참여 상태 구성
--            size=20 요청 시 첫 페이지 20개 처리 → 개선 전 1+(4×20)=81번 쿼리 확인
-- 버전 주의: migration/V4, V5 가 DDL로 사용 중이므로 V7 사용
-- =====================================================================

-- ① 회사 10개 추가 (company_id=2~11)
INSERT INTO companies (company_id, name, domain, created_at, updated_at)
VALUES
    ( 2, '네이버',     'naver.com',       NOW(), NOW()),
    ( 3, '카카오',     'kakao.com',       NOW(), NOW()),
    ( 4, '라인',       'linecorp.com',    NOW(), NOW()),
    ( 5, '쿠팡',       'coupang.com',     NOW(), NOW()),
    ( 6, '토스',       'toss.im',         NOW(), NOW()),
    ( 7, '배달의민족', 'baemin.com',      NOW(), NOW()),
    ( 8, '당근마켓',   'daangn.com',      NOW(), NOW()),
    ( 9, '하이퍼커넥트', 'hyperconnect.com', NOW(), NOW()),
    (10, '직방',       'zigbang.com',     NOW(), NOW()),
    (11, '야놀자',     'yanolja.com',     NOW(), NOW());

-- ② 채용공고(job_master) 20개 추가 (job_master_id=2~21)
INSERT INTO job_masters (job_master_id, company_id, job_title, status, created_at, updated_at)
VALUES
    ( 2,  2, '백엔드 개발자',       'OPEN', NOW(), NOW()),
    ( 3,  3, '서버 개발자',         'OPEN', NOW(), NOW()),
    ( 4,  4, '백엔드 엔지니어',     'OPEN', NOW(), NOW()),
    ( 5,  5, '플랫폼 개발자',       'OPEN', NOW(), NOW()),
    ( 6,  6, '서버 엔지니어',       'OPEN', NOW(), NOW()),
    ( 7,  7, '백엔드 개발자',       'OPEN', NOW(), NOW()),
    ( 8,  8, '서버 개발자',         'OPEN', NOW(), NOW()),
    ( 9,  9, 'Java 백엔드 개발자',  'OPEN', NOW(), NOW()),
    (10, 10, 'Spring 개발자',       'OPEN', NOW(), NOW()),
    (11, 11, '백엔드 플랫폼 개발자','OPEN', NOW(), NOW()),
    (12,  2, '신입 백엔드 개발자',  'OPEN', NOW(), NOW()),
    (13,  3, '경력 서버 개발자',    'OPEN', NOW(), NOW()),
    (14,  4, '인프라 백엔드',       'OPEN', NOW(), NOW()),
    (15,  5, '커머스 백엔드',       'OPEN', NOW(), NOW()),
    (16,  6, '핀테크 백엔드',       'OPEN', NOW(), NOW()),
    (17,  7, '딜리버리 백엔드',     'OPEN', NOW(), NOW()),
    (18,  8, '중고거래 백엔드',     'OPEN', NOW(), NOW()),
    (19,  9, '미디어 백엔드',       'OPEN', NOW(), NOW()),
    (20, 10, '부동산 백엔드',       'OPEN', NOW(), NOW()),
    (21, 11, '여가 백엔드',         'OPEN', NOW(), NOW());

-- ③ job_post 20개 추가 (job_post_id=2~21)
INSERT INTO job_posts (job_post_id, job_master_id, ai_job_id, company_id, source_type, source_url,
                       source_url_hash, raw_company_name, raw_job_title, recruitment_status,
                       registration_status, fingerprint_hash, created_at, updated_at)
VALUES
    ( 2,  2,  2,  2, 'MANUAL', 'https://naver.com/job/2',   SHA2('https://naver.com/job/2', 256),   '네이버',     '백엔드 개발자',       'OPEN', 'REGISTERED', SHA2('fp-2',  256), NOW(), NOW()),
    ( 3,  3,  3,  3, 'MANUAL', 'https://kakao.com/job/3',   SHA2('https://kakao.com/job/3', 256),   '카카오',     '서버 개발자',         'OPEN', 'REGISTERED', SHA2('fp-3',  256), NOW(), NOW()),
    ( 4,  4,  4,  4, 'MANUAL', 'https://line.com/job/4',    SHA2('https://line.com/job/4', 256),    '라인',       '백엔드 엔지니어',     'OPEN', 'REGISTERED', SHA2('fp-4',  256), NOW(), NOW()),
    ( 5,  5,  5,  5, 'MANUAL', 'https://coupang.com/job/5', SHA2('https://coupang.com/job/5', 256), '쿠팡',       '플랫폼 개발자',       'OPEN', 'REGISTERED', SHA2('fp-5',  256), NOW(), NOW()),
    ( 6,  6,  6,  6, 'MANUAL', 'https://toss.im/job/6',     SHA2('https://toss.im/job/6', 256),     '토스',       '서버 엔지니어',       'OPEN', 'REGISTERED', SHA2('fp-6',  256), NOW(), NOW()),
    ( 7,  7,  7,  7, 'MANUAL', 'https://baemin.com/job/7',  SHA2('https://baemin.com/job/7', 256),  '배달의민족', '백엔드 개발자',       'OPEN', 'REGISTERED', SHA2('fp-7',  256), NOW(), NOW()),
    ( 8,  8,  8,  8, 'MANUAL', 'https://daangn.com/job/8',  SHA2('https://daangn.com/job/8', 256),  '당근마켓',   '서버 개발자',         'OPEN', 'REGISTERED', SHA2('fp-8',  256), NOW(), NOW()),
    ( 9,  9,  9,  9, 'MANUAL', 'https://hyper.com/job/9',   SHA2('https://hyper.com/job/9', 256),   '하이퍼커넥트','Java 백엔드 개발자', 'OPEN', 'REGISTERED', SHA2('fp-9',  256), NOW(), NOW()),
    (10, 10, 10, 10, 'MANUAL', 'https://zigbang.com/job/10',SHA2('https://zigbang.com/job/10', 256),'직방',       'Spring 개발자',       'OPEN', 'REGISTERED', SHA2('fp-10', 256), NOW(), NOW()),
    (11, 11, 11, 11, 'MANUAL', 'https://yanolja.com/job/11',SHA2('https://yanolja.com/job/11', 256),'야놀자',     '백엔드 플랫폼 개발자','OPEN', 'REGISTERED', SHA2('fp-11', 256), NOW(), NOW()),
    (12, 12, 12,  2, 'MANUAL', 'https://naver.com/job/12',  SHA2('https://naver.com/job/12', 256),  '네이버',     '신입 백엔드 개발자',  'OPEN', 'REGISTERED', SHA2('fp-12', 256), NOW(), NOW()),
    (13, 13, 13,  3, 'MANUAL', 'https://kakao.com/job/13',  SHA2('https://kakao.com/job/13', 256),  '카카오',     '경력 서버 개발자',    'OPEN', 'REGISTERED', SHA2('fp-13', 256), NOW(), NOW()),
    (14, 14, 14,  4, 'MANUAL', 'https://line.com/job/14',   SHA2('https://line.com/job/14', 256),   '라인',       '인프라 백엔드',       'OPEN', 'REGISTERED', SHA2('fp-14', 256), NOW(), NOW()),
    (15, 15, 15,  5, 'MANUAL', 'https://coupang.com/job/15',SHA2('https://coupang.com/job/15', 256),'쿠팡',       '커머스 백엔드',       'OPEN', 'REGISTERED', SHA2('fp-15', 256), NOW(), NOW()),
    (16, 16, 16,  6, 'MANUAL', 'https://toss.im/job/16',    SHA2('https://toss.im/job/16', 256),    '토스',       '핀테크 백엔드',       'OPEN', 'REGISTERED', SHA2('fp-16', 256), NOW(), NOW()),
    (17, 17, 17,  7, 'MANUAL', 'https://baemin.com/job/17', SHA2('https://baemin.com/job/17', 256), '배달의민족', '딜리버리 백엔드',     'OPEN', 'REGISTERED', SHA2('fp-17', 256), NOW(), NOW()),
    (18, 18, 18,  8, 'MANUAL', 'https://daangn.com/job/18', SHA2('https://daangn.com/job/18', 256), '당근마켓',   '중고거래 백엔드',     'OPEN', 'REGISTERED', SHA2('fp-18', 256), NOW(), NOW()),
    (19, 19, 19,  9, 'MANUAL', 'https://hyper.com/job/19',  SHA2('https://hyper.com/job/19', 256),  '하이퍼커넥트','미디어 백엔드',      'OPEN', 'REGISTERED', SHA2('fp-19', 256), NOW(), NOW()),
    (20, 20, 20, 10, 'MANUAL', 'https://zigbang.com/job/20',SHA2('https://zigbang.com/job/20', 256),'직방',       '부동산 백엔드',       'OPEN', 'REGISTERED', SHA2('fp-20', 256), NOW(), NOW()),
    (21, 21, 21, 11, 'MANUAL', 'https://yanolja.com/job/21',SHA2('https://yanolja.com/job/21', 256),'야놀자',     '여가 백엔드',         'OPEN', 'REGISTERED', SHA2('fp-21', 256), NOW(), NOW());

-- ④ userId=1의 지원서 20개 추가 (job_application_id=3~22)
-- V2에서 이미 job_application_id=1(userId=1), 2(userId=2) 사용
INSERT INTO job_applications (job_application_id, user_id, job_master_id, status, created_at, updated_at)
VALUES
    ( 3, 1,  2, 'SUBMITTED', NOW(), NOW()),
    ( 4, 1,  3, 'SUBMITTED', NOW(), NOW()),
    ( 5, 1,  4, 'SUBMITTED', NOW(), NOW()),
    ( 6, 1,  5, 'SUBMITTED', NOW(), NOW()),
    ( 7, 1,  6, 'SUBMITTED', NOW(), NOW()),
    ( 8, 1,  7, 'SUBMITTED', NOW(), NOW()),
    ( 9, 1,  8, 'SUBMITTED', NOW(), NOW()),
    (10, 1,  9, 'SUBMITTED', NOW(), NOW()),
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

-- ⑤ AI 평가 20개 추가 (evaluation_id=3~22, score=80~93)
-- V2에서 이미 evaluation_id=1,2 사용
-- comparison_scores: List<EvaluationScore> → JSON 배열 '[]' 필수
INSERT INTO ai_applicant_evaluation (evaluation_id, job_application_id, overall_score,
                                     one_line_review, feedback_detail, comparison_scores, created_at, updated_at)
VALUES
    ( 3,  3, 80, '가능성 있는 개발자', '성장 가능성이 높습니다', '[]', NOW(), NOW()),
    ( 4,  4, 81, '성실한 개발자',     '꼼꼼한 코드 작성 능력', '[]', NOW(), NOW()),
    ( 5,  5, 82, '역량 있는 개발자',  '실력이 검증됐습니다',    '[]', NOW(), NOW()),
    ( 6,  6, 83, '우수한 개발자',     '기술력이 뛰어납니다',    '[]', NOW(), NOW()),
    ( 7,  7, 84, '탁월한 개발자',     '문제 해결 능력 우수',    '[]', NOW(), NOW()),
    ( 8,  8, 85, '뛰어난 개발자',     '코드 품질이 높습니다',   '[]', NOW(), NOW()),
    ( 9,  9, 86, '경쟁력 있는 개발자','기술 스택이 좋습니다',   '[]', NOW(), NOW()),
    (10, 10, 87, '실력 있는 개발자',  '경험이 풍부합니다',      '[]', NOW(), NOW()),
    (11, 11, 88, '우수한 백엔드 개발자','서버 경험이 많습니다',  '[]', NOW(), NOW()),
    (12, 12, 89, '뛰어난 백엔드 개발자','최신 기술에 밝습니다',  '[]', NOW(), NOW()),
    (13, 13, 90, '최고 수준 개발자',  '압도적인 실력입니다',    '[]', NOW(), NOW()),
    (14, 14, 91, '엘리트 개발자',     '팀에 큰 도움이 됩니다',  '[]', NOW(), NOW()),
    (15, 15, 92, '상위권 개발자',     '전반적으로 우수합니다',  '[]', NOW(), NOW()),
    (16, 16, 93, '상위 1% 개발자',    '기대 이상의 실력',       '[]', NOW(), NOW()),
    (17, 17, 80, '꾸준한 개발자',     '지속적인 성장세',         '[]', NOW(), NOW()),
    (18, 18, 81, '성장하는 개발자',   '매우 빠른 성장률',        '[]', NOW(), NOW()),
    (19, 19, 82, '잠재력 있는 개발자','앞으로가 기대됩니다',     '[]', NOW(), NOW()),
    (20, 20, 83, '노력하는 개발자',   '열정이 대단합니다',       '[]', NOW(), NOW()),
    (21, 21, 84, '적극적인 개발자',   '도전 정신이 돋보입니다',  '[]', NOW(), NOW()),
    (22, 22, 85, '협업을 잘하는 개발자','팀워크가 훌륭합니다',   '[]', NOW(), NOW());

-- ⑥ 채팅방 20개 추가 (chat_room_id=2~21, 방장=userId=1)
-- V2에서 이미 chat_room_id=1 사용
-- status: RoomStatus enum → 'ACTIVE' 또는 'CLOSED'
INSERT INTO chat_rooms (chat_room_id, job_master_id, created_by, room_name, max_participants,
                        room_goal, cutline_score, preferred_conditions, status, created_at, updated_at)
VALUES
    ( 2,  2, 1, '네이버 백엔드 스터디',     5, 'INTERVIEW', 75, NULL, 'ACTIVE', NOW(), NOW()),
    ( 3,  3, 1, '카카오 서버 스터디',       5, 'INTERVIEW', 76, NULL, 'ACTIVE', NOW(), NOW()),
    ( 4,  4, 1, '라인 백엔드 스터디',       5, 'DOCUMENT',  77, NULL, 'ACTIVE', NOW(), NOW()),
    ( 5,  5, 1, '쿠팡 플랫폼 스터디',       5, 'DOCUMENT',  78, NULL, 'ACTIVE', NOW(), NOW()),
    ( 6,  6, 1, '토스 서버 스터디',         5, 'INTERVIEW', 79, NULL, 'ACTIVE', NOW(), NOW()),
    ( 7,  7, 1, '배민 백엔드 스터디',       5, 'INTERVIEW', 80, NULL, 'ACTIVE', NOW(), NOW()),
    ( 8,  8, 1, '당근 서버 스터디',         5, 'DOCUMENT',  75, NULL, 'ACTIVE', NOW(), NOW()),
    ( 9,  9, 1, '하이퍼커넥트 스터디',      5, 'INTERVIEW', 76, NULL, 'ACTIVE', NOW(), NOW()),
    (10, 10, 1, '직방 Spring 스터디',       5, 'DOCUMENT',  77, NULL, 'ACTIVE', NOW(), NOW()),
    (11, 11, 1, '야놀자 백엔드 스터디',     5, 'INTERVIEW', 78, NULL, 'ACTIVE', NOW(), NOW()),
    (12, 12, 1, '네이버 신입 스터디',       5, 'DOCUMENT',  79, NULL, 'ACTIVE', NOW(), NOW()),
    (13, 13, 1, '카카오 경력 스터디',       5, 'INTERVIEW', 80, NULL, 'ACTIVE', NOW(), NOW()),
    (14, 14, 1, '라인 인프라 스터디',       5, 'DOCUMENT',  75, NULL, 'ACTIVE', NOW(), NOW()),
    (15, 15, 1, '쿠팡 커머스 스터디',       5, 'INTERVIEW', 76, NULL, 'ACTIVE', NOW(), NOW()),
    (16, 16, 1, '토스 핀테크 스터디',       5, 'INTERVIEW', 77, NULL, 'ACTIVE', NOW(), NOW()),
    (17, 17, 1, '배민 딜리버리 스터디',     5, 'DOCUMENT',  78, NULL, 'ACTIVE', NOW(), NOW()),
    (18, 18, 1, '당근 중고거래 스터디',     5, 'INTERVIEW', 79, NULL, 'ACTIVE', NOW(), NOW()),
    (19, 19, 1, '하이퍼 미디어 스터디',     5, 'DOCUMENT',  80, NULL, 'ACTIVE', NOW(), NOW()),
    (20, 20, 1, '직방 부동산 스터디',       5, 'INTERVIEW', 75, NULL, 'ACTIVE', NOW(), NOW()),
    (21, 21, 1, '야놀자 여가 스터디',       5, 'DOCUMENT',  76, NULL, 'ACTIVE', NOW(), NOW());

-- ⑦ chat_room_members 20개 추가 (userId=1, HOST)
-- V2에서 이미 chat_room_member_id=1(HOST), 2(MEMBER) 사용
INSERT INTO chat_room_members (chat_room_member_id, chat_room_id, user_id, job_application_id, role, joined_at)
VALUES
    ( 3,  2, 1,  3, 'HOST', NOW()),
    ( 4,  3, 1,  4, 'HOST', NOW()),
    ( 5,  4, 1,  5, 'HOST', NOW()),
    ( 6,  5, 1,  6, 'HOST', NOW()),
    ( 7,  6, 1,  7, 'HOST', NOW()),
    ( 8,  7, 1,  8, 'HOST', NOW()),
    ( 9,  8, 1,  9, 'HOST', NOW()),
    (10,  9, 1, 10, 'HOST', NOW()),
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
