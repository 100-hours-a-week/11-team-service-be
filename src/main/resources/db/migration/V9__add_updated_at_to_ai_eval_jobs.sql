ALTER TABLE ai_eval_jobs ADD COLUMN updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
UPDATE ai_eval_jobs SET updated_at = created_at;
