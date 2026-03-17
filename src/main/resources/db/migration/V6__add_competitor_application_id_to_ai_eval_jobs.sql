-- COMPARISON 타입 비동기 처리를 위해 ai_eval_jobs에 competitor_application_id 추가
--
-- 추가 이유:
--   AI 비교 분석 비동기 전환 시 콜백(/api/internal/ai/callback) 수신 시점에
--   ai_applicant_comparison 저장을 위해 my_application_id + competitor_application_id
--   두 값이 모두 필요하다. eval_job_id만으로는 my_application_id만 식별 가능하므로
--   COMPARISON 타입 AiEvalJob 생성 시 경쟁자 지원 ID를 함께 저장한다.
--   EVALUATION/RESUME/PORTFOLIO 타입에서는 해당 컬럼을 사용하지 않으므로 NULL 허용.
ALTER TABLE ai_eval_jobs
    ADD COLUMN competitor_application_id BIGINT NULL AFTER job_application_id,
    ADD CONSTRAINT fk_ai_eval_jobs_competitor_application
        FOREIGN KEY (competitor_application_id) REFERENCES job_applications (job_application_id);
