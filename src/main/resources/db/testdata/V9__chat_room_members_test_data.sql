-- =====================================================================
-- [로컬 개발 전용] getChatRoomMembers N+1 테스트용 멤버 시드
-- 목적: chat_room_id=1의 멤버를 최대 정원(5명)으로 구성하여 N+1 최악 케이스 재현
--
-- 의존성: V2 (chat_room_id=1, userId=1 HOST, userId=2 MEMBER 존재 필수)
--         V8 (userId=3,4,5 및 job_application_id=3,4,5 존재 필수)
-- 설계 의도: 기존 멤버(userId=1 HOST, userId=2 MEMBER) + 3명 추가 = 총 5명 (최대 정원)
--            최악의 경우(N=5) 재현으로 개선 전 3+5=8번 쿼리 확인
--            조회 주체 userId=1이 HOST이므로 권한 확인 통과
-- =====================================================================

-- chat_room_id=1에 userId=3,4,5를 MEMBER로 추가
-- V2에서 chat_room_member_id=1(userId=1 HOST), 2(userId=2 MEMBER) 사용
-- V8에서 job_application_id=3(userId=3), 4(userId=4), 5(userId=5) 생성
INSERT INTO chat_room_members (chat_room_member_id, chat_room_id, user_id, job_application_id, role, joined_at)
VALUES
    (3, 1, 3, 3, 'MEMBER', NOW()),
    (4, 1, 4, 4, 'MEMBER', NOW()),
    (5, 1, 5, 5, 'MEMBER', NOW());
