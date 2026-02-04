-- ================================================
-- 테스트 기업 데이터 (로컬 환경 전용)
-- ================================================
-- 의도: 채용공고 테스트를 위한 실제 기업명 제공
-- 근거: 기업 정규화 및 중복 처리 로직 테스트 가능
-- ================================================

-- Company 1: 대기업
INSERT INTO companies (company_id, name, domain, created_at, updated_at, deleted_at)
VALUES (1, '카카오', 'kakao.com', NOW(6), NOW(6), NULL);

-- Company 2: 중견기업
INSERT INTO companies (company_id, name, domain, created_at, updated_at, deleted_at)
VALUES (2, '토스', 'toss.im', NOW(6), NOW(6), NULL);

-- Company 3: 스타트업
INSERT INTO companies (company_id, name, domain, created_at, updated_at, deleted_at)
VALUES (3, '당근마켓', 'daangn.com', NOW(6), NOW(6), NULL);

-- ================================================
-- 회사 별칭 (Company Aliases)
-- ================================================
-- 의도: AI가 파싱한 다양한 회사명 표기를 정규화
-- 근거: "카카오", "Kakao", "KAKAO" 등을 동일 기업으로 인식
-- 참고: alias_normalized는 (company_id, alias_normalized) 복합 유니크 키
-- ================================================

-- 카카오 별칭
INSERT INTO company_aliases (alias_id, company_id, source, alias_name, alias_normalized, created_at, updated_at, deleted_at)
VALUES (1, 1, 'AI_PARSED', 'Kakao', 'kakao_en', NOW(6), NOW(6), NULL);

INSERT INTO company_aliases (alias_id, company_id, source, alias_name, alias_normalized, created_at, updated_at, deleted_at)
VALUES (2, 1, 'AI_PARSED', '카카오 주식회사', 'kakao_corp', NOW(6), NOW(6), NULL);

-- 토스 별칭
INSERT INTO company_aliases (alias_id, company_id, source, alias_name, alias_normalized, created_at, updated_at, deleted_at)
VALUES (3, 2, 'AI_PARSED', 'Toss', 'toss_en', NOW(6), NOW(6), NULL);

INSERT INTO company_aliases (alias_id, company_id, source, alias_name, alias_normalized, created_at, updated_at, deleted_at)
VALUES (4, 2, 'AI_PARSED', '비바리퍼블리카', 'viva_republica', NOW(6), NOW(6), NULL);

-- 당근마켓 별칭
INSERT INTO company_aliases (alias_id, company_id, source, alias_name, alias_normalized, created_at, updated_at, deleted_at)
VALUES (5, 3, 'AI_PARSED', 'Daangn', 'daangn_en', NOW(6), NOW(6), NULL);

INSERT INTO company_aliases (alias_id, company_id, source, alias_name, alias_normalized, created_at, updated_at, deleted_at)
VALUES (6, 3, 'AI_PARSED', '당근', 'daangn_short', NOW(6), NOW(6), NULL);
