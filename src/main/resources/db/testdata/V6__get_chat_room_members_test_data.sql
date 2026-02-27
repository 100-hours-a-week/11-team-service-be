-- =====================================================================
-- [로컬 개발 전용] getChatRoomMembers N+1 성능 테스트용 시드 데이터
-- 목적: chat_room_id=1에 멤버를 최대 정원(5명)으로 채워
--       멤버 수에 비례한 N+1 쿼리 문제를 수치로 확인하기 위함
-- 구조:
--   - 채팅방 최대 정원: 5명 (방장 포함)
--   - 기존 멤버: userId=1(HOST), userId=2(MEMBER) → 2명 존재
--   - 추가 멤버: userId=3,4,5 → 3명 추가 → 총 5명 (정원 꽉 참)
--   - getChatRoomMembers 조회 시 멤버 1명당 userRepository.findNicknameByUserId 1번 실행
--   - 멤버 5명 기준: 3(고정) + 5(N) = 8번 쿼리
--     ├── chatRoomRepository.findByIdNotDeleted                       → 1번
--     ├── chatRoomMemberRepository.findByChatRoomIdAndUserId          → 1번 (권한 확인)
--     ├── chatRoomMemberRepository.findAllActiveMembersByChatRoomId   → 1번
--     └── userRepository.findNicknameByUserId                         → N번 (멤버마다 반복)
-- 주의: V5 데이터가 먼저 적용되어 있어야 함 (유저 3~12 존재)
-- =====================================================================

-- ① 유저 3,4,5를 chat_room_id=1의 MEMBER로 추가
--    기존 멤버: userId=1(HOST), userId=2(MEMBER) → 총 5명으로 늘림 (정원 꽉 참)
--    chat_room_member_id=33~35 (V5의 23~32와 겹치지 않게)
INSERT INTO chat_room_members (chat_room_member_id, chat_room_id, user_id, job_application_id, role, joined_at) VALUES
    (33, 1, 3, 23, 'MEMBER', NOW()),
    (34, 1, 4, 24, 'MEMBER', NOW()),
    (35, 1, 5, 25, 'MEMBER', NOW());
