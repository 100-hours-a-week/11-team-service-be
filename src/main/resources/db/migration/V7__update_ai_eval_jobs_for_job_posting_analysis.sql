ALTER TABLE ai_eval_jobs MODIFY COLUMN job_application_id BIGINT NULL;
ALTER TABLE ai_eval_jobs ADD COLUMN source_url VARCHAR(512) NULL;
