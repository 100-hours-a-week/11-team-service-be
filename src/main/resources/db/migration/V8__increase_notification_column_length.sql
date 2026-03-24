-- V8__increase_notification_column_length.sql
-- 알림 제목(title)과 본문(body)의 길이를 확장합니다.
ALTER TABLE notifications MODIFY COLUMN title VARCHAR(100);
ALTER TABLE notifications MODIFY COLUMN body VARCHAR(255);
