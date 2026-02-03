-- ================================================
-- 테스트 채용공고 데이터 (로컬 환경 전용)
-- ================================================
-- 의도: 채용공고 조회, 지원, AI 분석 기능 테스트
-- 근거: 실제 공고 데이터 없이도 전체 워크플로우 테스트 가능
-- ================================================

-- Job Master 1: 카카오 백엔드 개발자
INSERT INTO job_masters (job_master_id, company_id, job_title, main_tasks, start_date, end_date, ai_summary, evaluation_criteria, status, last_seen_at, created_at, updated_at, deleted_at)
VALUES (
    1,
    1,
    '백엔드 개발자',
    '["RESTful API 설계 및 개발", "데이터베이스 스키마 설계 및 최적화", "마이크로서비스 아키텍처 구축"]',
    '2026-01-01',
    '2026-03-31',
    '클라우드 기반 마이크로서비스 아키텍처 경험을 가진 백엔드 개발자를 찾고 있으며, Java/Spring 생태계에 대한 깊은 이해가 필요합니다.',
    '{"technical_depth": 40, "problem_solving": 30, "communication": 20, "experience": 10}',
    'OPEN',
    NOW(6),
    NOW(6),
    NOW(6),
    NULL
);

-- Job Master 2: 토스 프론트엔드 개발자
INSERT INTO job_masters (job_master_id, company_id, job_title, main_tasks, start_date, end_date, ai_summary, evaluation_criteria, status, last_seen_at, created_at, updated_at, deleted_at)
VALUES (
    2,
    2,
    '프론트엔드 개발자',
    '["React 기반 UI/UX 개발", "성능 최적화 및 번들 사이즈 관리", "디자인 시스템 구축 및 유지보수"]',
    '2026-01-15',
    '2026-04-15',
    'React/Next.js 기반의 현대적인 웹 애플리케이션 개발 경험이 있는 프론트엔드 개발자를 찾고 있습니다.',
    '{"technical_depth": 35, "ui_ux_sense": 25, "problem_solving": 25, "team_collaboration": 15}',
    'OPEN',
    NOW(6),
    NOW(6),
    NOW(6),
    NULL
);

-- Job Master 3: 당근마켓 풀스택 개발자
INSERT INTO job_masters (job_master_id, company_id, job_title, main_tasks, start_date, end_date, ai_summary, evaluation_criteria, status, last_seen_at, created_at, updated_at, deleted_at)
VALUES (
    3,
    3,
    '풀스택 개발자',
    '["백엔드 API 및 프론트엔드 UI 개발", "서비스 기획부터 배포까지 전 과정 참여", "A/B 테스트 및 데이터 분석"]',
    '2026-02-01',
    '2026-05-31',
    '스타트업 환경에서 빠르게 성장하고 싶은 풀스택 개발자를 찾고 있습니다. 기획부터 배포까지 주도적으로 진행할 수 있는 분을 환영합니다.',
    '{"technical_versatility": 30, "ownership": 30, "speed": 20, "growth_potential": 20}',
    'OPEN',
    NOW(6),
    NOW(6),
    NOW(6),
    NULL
);

-- ================================================
-- Job Master와 Skill 연결
-- ================================================
-- 의도: 채용공고별 요구 기술 스택 매핑
-- 근거: 기술 기반 공고 검색 및 매칭 기능 테스트
-- ================================================

-- 카카오 백엔드 (Job Master 1)
INSERT INTO job_master_skills (job_master_id, skill_id, created_at, deleted_at)
VALUES (1, 1, NOW(6), NULL),  -- Java
       (1, 2, NOW(6), NULL),  -- Spring Boot
       (1, 3, NOW(6), NULL),  -- JPA
       (1, 4, NOW(6), NULL),  -- MySQL
       (1, 10, NOW(6), NULL), -- Docker
       (1, 12, NOW(6), NULL); -- AWS

-- 토스 프론트엔드 (Job Master 2)
INSERT INTO job_master_skills (job_master_id, skill_id, created_at, deleted_at)
VALUES (2, 7, NOW(6), NULL),  -- TypeScript
       (2, 8, NOW(6), NULL),  -- React
       (2, 9, NOW(6), NULL);  -- Next.js

-- 당근마켓 풀스택 (Job Master 3)
INSERT INTO job_master_skills (job_master_id, skill_id, created_at, deleted_at)
VALUES (3, 1, NOW(6), NULL),  -- Java
       (3, 2, NOW(6), NULL),  -- Spring Boot
       (3, 7, NOW(6), NULL),  -- TypeScript
       (3, 8, NOW(6), NULL),  -- React
       (3, 10, NOW(6), NULL); -- Docker

-- ================================================
-- Job Posts (개별 URL 공고)
-- ================================================
-- 의도: job_masters와 job_posts 관계 테스트
-- 근거: 중복 공고 처리 로직 검증
-- ================================================

-- 카카오 백엔드 - 원본 공고
INSERT INTO job_posts (job_post_id, job_master_id, ai_job_id, company_id, created_by, source_type, source_url, source_url_hash, raw_company_name, raw_job_title, main_tasks, recruitment_status, registration_status, start_date, end_date, created_at, updated_at, deleted_at, fingerprint_hash)
VALUES (
    1,
    1,
    12345,
    1,
    2,
    'USER_SUBMITTED',
    'https://careers.kakao.com/jobs/12345',
    SHA2('https://careers.kakao.com/jobs/12345', 256),
    '카카오',
    '백엔드 개발자',
    '["RESTful API 설계 및 개발", "데이터베이스 스키마 설계 및 최적화"]',
    'RECRUITING',
    'APPROVED',
    '2026-01-01',
    '2026-03-31',
    NOW(6),
    NOW(6),
    NULL,
    SHA2(CONCAT('카카오', '백엔드 개발자', 'RESTful API 설계 및 개발'), 256)
);

-- 토스 프론트엔드 - 원본 공고
INSERT INTO job_posts (job_post_id, job_master_id, ai_job_id, company_id, created_by, source_type, source_url, source_url_hash, raw_company_name, raw_job_title, main_tasks, recruitment_status, registration_status, start_date, end_date, created_at, updated_at, deleted_at, fingerprint_hash)
VALUES (
    2,
    2,
    23456,
    2,
    2,
    'USER_SUBMITTED',
    'https://toss.im/career/job/23456',
    SHA2('https://toss.im/career/job/23456', 256),
    '토스',
    '프론트엔드 개발자',
    '["React 기반 UI/UX 개발", "성능 최적화"]',
    'RECRUITING',
    'APPROVED',
    '2026-01-15',
    '2026-04-15',
    NOW(6),
    NOW(6),
    NULL,
    SHA2(CONCAT('토스', '프론트엔드 개발자', 'React 기반 UI/UX 개발'), 256)
);

-- 당근마켓 풀스택 - 원본 공고
INSERT INTO job_posts (job_post_id, job_master_id, ai_job_id, company_id, created_by, source_type, source_url, source_url_hash, raw_company_name, raw_job_title, main_tasks, recruitment_status, registration_status, start_date, end_date, created_at, updated_at, deleted_at, fingerprint_hash)
VALUES (
    3,
    3,
    34567,
    3,
    3,
    'USER_SUBMITTED',
    'https://team.daangn.com/jobs/34567',
    SHA2('https://team.daangn.com/jobs/34567', 256),
    '당근마켓',
    '풀스택 개발자',
    '["백엔드 API 및 프론트엔드 UI 개발"]',
    'RECRUITING',
    'APPROVED',
    '2026-02-01',
    '2026-05-31',
    NOW(6),
    NOW(6),
    NULL,
    SHA2(CONCAT('당근마켓', '풀스택 개발자', '백엔드 API 및 프론트엔드 UI 개발'), 256)
);
