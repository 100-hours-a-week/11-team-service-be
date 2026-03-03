-- =====================================================================
-- V4: chat_messages content 컬럼 NULL 허용으로 변경
--
-- 변경 이유:
--   채팅 도메인 테크스펙에 "파일만 전송 시 content NULL 허용" 정책이
--   명시되어 있으나, 테이블 정의서에 NOT NULL로 잘못 기재되어 DDL에
--   반영됨. messageType=FILE인 경우 content=null로 INSERT 시
--   DB 제약 위반으로 500 에러 발생.
--
-- 영향 범위:
--   chat_messages.content: VARCHAR(1000) NOT NULL → TEXT NULL
--   (TEXT로 변경한 이유: 테이블 정의서 원본 설계 의도가 TEXT 타입이었으며,
--    VARCHAR(1000)은 V1 DDL 작성 시 잘못 반영된 것)
--
-- 실행 환경: 로컬 / 스테이징 / Prod 동일 적용
-- =====================================================================

ALTER TABLE chat_messages
    MODIFY COLUMN content TEXT NULL;
