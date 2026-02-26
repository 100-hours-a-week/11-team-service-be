-- =====================================================================
-- V3: chat_room_members UNIQUE 제약 변경
--
-- 변경 이유:
--   기존 단독 UNIQUE(job_application_id)는 "동일 지원서로 평생 어떤 채팅방도
--   재입장 불가"가 되어 비즈니스 로직과 불일치.
--   의도된 정책은 "동일 지원서로 동일 채팅방에 중복 입장 불가"이므로
--   복합 UNIQUE(job_application_id, chat_room_id)로 변경.
--
-- 실행 환경: 로컬(DB 새로 구성) 및 Prod 양쪽 모두 적용
-- =====================================================================

-- ① FK 제약 임시 DROP
--    MySQL은 인덱스가 FK를 지원하고 있으면 인덱스를 직접 DROP할 수 없음
--    → FK를 먼저 제거해야 인덱스 변경 가능
ALTER TABLE chat_room_members DROP FOREIGN KEY fk_chat_members_application;

-- ② 기존 단독 UNIQUE 인덱스 DROP
--    로컬(V1 생성): uk_chat_members_application
--    Prod(JPA 자동생성): UKc3ybgeatw4udbsiu4uplrmbjp → Prod는 이 파일 대신 별도 SQL로 처리
ALTER TABLE chat_room_members DROP INDEX uk_chat_members_application;

-- ③ 복합 UNIQUE 제약 추가
--    (job_application_id, chat_room_id) 조합 중복 시 INSERT 차단
--    → "같은 지원서로 같은 방에 두 번 들어올 수 없다"는 정책을 DB 레벨에서 보장
ALTER TABLE chat_room_members
    ADD CONSTRAINT uk_chat_members_application_room
        UNIQUE (job_application_id, chat_room_id);

-- ④ FK 제약 재등록
--    인덱스 변경 완료 후 FK를 복합 UNIQUE 위에 다시 붙임
--    MySQL은 FK 지원 인덱스로 UNIQUE 인덱스를 자동 인식함
ALTER TABLE chat_room_members
    ADD CONSTRAINT fk_chat_members_application
        FOREIGN KEY (job_application_id) REFERENCES job_applications (job_application_id);
