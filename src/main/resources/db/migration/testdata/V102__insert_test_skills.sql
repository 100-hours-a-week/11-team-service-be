-- ================================================
-- 테스트 기술 스택 데이터 (로컬 환경 전용)
-- ================================================
-- 의도: 채용공고에 사용되는 기술 스택 기준 데이터 제공
-- 근거: 기술 검색, 필터링, 매칭 기능 테스트 가능
-- ================================================

-- 백엔드 기술
INSERT INTO skills (skill_id, skill_name, created_at, updated_at, deleted_at)
VALUES (1, 'Java', NOW(6), NOW(6), NULL);

INSERT INTO skills (skill_id, skill_name, created_at, updated_at, deleted_at)
VALUES (2, 'Spring Boot', NOW(6), NOW(6), NULL);

INSERT INTO skills (skill_id, skill_name, created_at, updated_at, deleted_at)
VALUES (3, 'JPA', NOW(6), NOW(6), NULL);

INSERT INTO skills (skill_id, skill_name, created_at, updated_at, deleted_at)
VALUES (4, 'MySQL', NOW(6), NOW(6), NULL);

INSERT INTO skills (skill_id, skill_name, created_at, updated_at, deleted_at)
VALUES (5, 'Redis', NOW(6), NOW(6), NULL);

-- 프론트엔드 기술
INSERT INTO skills (skill_id, skill_name, created_at, updated_at, deleted_at)
VALUES (6, 'JavaScript', NOW(6), NOW(6), NULL);

INSERT INTO skills (skill_id, skill_name, created_at, updated_at, deleted_at)
VALUES (7, 'TypeScript', NOW(6), NOW(6), NULL);

INSERT INTO skills (skill_id, skill_name, created_at, updated_at, deleted_at)
VALUES (8, 'React', NOW(6), NOW(6), NULL);

INSERT INTO skills (skill_id, skill_name, created_at, updated_at, deleted_at)
VALUES (9, 'Next.js', NOW(6), NOW(6), NULL);

-- 인프라/데브옵스
INSERT INTO skills (skill_id, skill_name, created_at, updated_at, deleted_at)
VALUES (10, 'Docker', NOW(6), NOW(6), NULL);

INSERT INTO skills (skill_id, skill_name, created_at, updated_at, deleted_at)
VALUES (11, 'Kubernetes', NOW(6), NOW(6), NULL);

INSERT INTO skills (skill_id, skill_name, created_at, updated_at, deleted_at)
VALUES (12, 'AWS', NOW(6), NOW(6), NULL);

-- ================================================
-- 기술 스택 별칭 (Skill Aliases)
-- ================================================
-- 의도: AI가 파싱한 다양한 기술명 표기를 정규화
-- 근거: "spring", "Spring", "SpringBoot" 등을 동일 기술로 인식
-- 참고: alias_normalized는 (skill_id, alias_normalized) 복합 유니크 키
-- ================================================

-- Spring Boot 별칭
INSERT INTO skill_aliases (alias_id, skill_id, alias_name, alias_normalized, created_at, updated_at, deleted_at)
VALUES (1, 2, 'spring', 'spring_lowercase', NOW(6), NOW(6), NULL);

INSERT INTO skill_aliases (alias_id, skill_id, alias_name, alias_normalized, created_at, updated_at, deleted_at)
VALUES (2, 2, 'SpringBoot', 'springboot', NOW(6), NOW(6), NULL);

-- React 별칭
INSERT INTO skill_aliases (alias_id, skill_id, alias_name, alias_normalized, created_at, updated_at, deleted_at)
VALUES (3, 8, 'react.js', 'react_js', NOW(6), NOW(6), NULL);

INSERT INTO skill_aliases (alias_id, skill_id, alias_name, alias_normalized, created_at, updated_at, deleted_at)
VALUES (4, 8, 'ReactJS', 'reactjs', NOW(6), NOW(6), NULL);

-- Next.js 별칭
INSERT INTO skill_aliases (alias_id, skill_id, alias_name, alias_normalized, created_at, updated_at, deleted_at)
VALUES (5, 9, 'nextjs', 'nextjs_lowercase', NOW(6), NOW(6), NULL);

INSERT INTO skill_aliases (alias_id, skill_id, alias_name, alias_normalized, created_at, updated_at, deleted_at)
VALUES (6, 9, 'Next', 'next_short', NOW(6), NOW(6), NULL);
