-- [이슈8] AI 비교 결과 중복 저장 방지를 위한 UNIQUE 제약 추가
--
-- 배경:
--   ChatMemberComparisonService.compare()에서
--   findBy → AI 호출(수 초) → save() 구조에 race condition 존재.
--   동시 요청 시 두 요청 모두 "결과 없음"으로 판단해 AI를 중복 호출하고
--   동일한 (my_application_id, competitor_application_id) 조합을 2건 저장함.
--
-- 해결:
--   DB 레벨에서 중복 저장을 차단하는 UNIQUE 제약 추가.
--   나중에 INSERT된 건은 DataIntegrityViolationException 발생 →
--   서비스 레이어에서 catch 후 기존 결과를 조회해 반환.
--
-- 참고:
--   기존 idx_comparison_my_competitor 인덱스는 조회 최적화 전용이었음.
--   UNIQUE 제약은 별도로 추가해야 중복 방지 효과가 생김.

ALTER TABLE ai_applicant_comparison
    ADD CONSTRAINT uq_comparison_my_competitor
        UNIQUE (my_application_id, competitor_application_id);
