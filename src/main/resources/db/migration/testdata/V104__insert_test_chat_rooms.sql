-- ================================================
-- 테스트 채용 지원 데이터 (로컬 환경 전용)
-- ================================================
-- 의도: 지원 현황, AI 평가, 채팅방 입장 조건 테스트
-- 근거: 지원자-공고 관계 및 점수 기반 입장 로직 검증
-- ================================================

-- User 2 (dev_kim)가 카카오 백엔드에 지원
INSERT INTO job_applications (job_application_id, user_id, job_master_id, status, created_at, updated_at, deleted_at)
VALUES (1, 2, 1, 'ACTIVE', NOW(6), NOW(6), NULL);

-- User 3 (frontend_park)가 토스 프론트엔드에 지원
INSERT INTO job_applications (job_application_id, user_id, job_master_id, status, created_at, updated_at, deleted_at)
VALUES (2, 3, 2, 'ACTIVE', NOW(6), NOW(6), NULL);

-- User 2 (dev_kim)가 당근마켓 풀스택에도 지원
INSERT INTO job_applications (job_application_id, user_id, job_master_id, status, created_at, updated_at, deleted_at)
VALUES (3, 2, 3, 'ACTIVE', NOW(6), NOW(6), NULL);

-- ================================================
-- AI 지원자 평가 (AI Applicant Evaluation)
-- ================================================
-- 의도: 채팅방 입장 컷라인 점수 비교 테스트
-- 근거: overall_score를 기준으로 입장 가능 여부 판단
-- ================================================

-- User 2의 카카오 백엔드 지원 평가 (점수: 85)
INSERT INTO ai_applicant_evaluation (evaluation_id, job_application_id, overall_score, one_line_review, feedback_detail, comparison_scores, created_at, updated_at, deleted_at)
VALUES (
    1,
    1,
    85,
    '탄탄한 백엔드 기술 스택과 프로젝트 경험을 보유한 지원자입니다.',
    'Java/Spring Boot 숙련도가 높고, 마이크로서비스 아키텍처 경험이 풍부합니다. 다만 대규모 트래픽 처리 경험을 더 쌓으면 좋을 것 같습니다.',
    '[{"name": "기술 역량", "description": "Java/Spring 숙련도 90점"}, {"name": "문제 해결 능력", "description": "시스템 설계 능력 85점"}, {"name": "커뮤니케이션", "description": "팀 협업 능력 80점"}, {"name": "경력", "description": "실무 경험 85점"}]',
    NOW(6),
    NOW(6),
    NULL
);

-- User 3의 토스 프론트엔드 지원 평가 (점수: 78)
INSERT INTO ai_applicant_evaluation (evaluation_id, job_application_id, overall_score, one_line_review, feedback_detail, comparison_scores, created_at, updated_at, deleted_at)
VALUES (
    2,
    2,
    78,
    'React/TypeScript 기반 프로젝트 경험이 우수한 지원자입니다.',
    'UI/UX에 대한 이해도가 높고 성능 최적화 경험이 있습니다. 디자인 시스템 구축 경험을 더 쌓으면 경쟁력이 높아질 것입니다.',
    '[{"name": "기술 역량", "description": "React/TypeScript 숙련도 80점"}, {"name": "UI/UX 감각", "description": "디자인 감각 85점"}, {"name": "문제 해결", "description": "최적화 능력 75점"}, {"name": "팀 협업", "description": "커뮤니케이션 72점"}]',
    NOW(6),
    NOW(6),
    NULL
);

-- User 2의 당근마켓 풀스택 지원 평가 (점수: 82)
INSERT INTO ai_applicant_evaluation (evaluation_id, job_application_id, overall_score, one_line_review, feedback_detail, comparison_scores, created_at, updated_at, deleted_at)
VALUES (
    3,
    3,
    82,
    '백엔드/프론트엔드 모두 다룰 수 있는 균형잡힌 개발자입니다.',
    '풀스택 역량이 우수하며 주도적으로 프로젝트를 이끌어본 경험이 있습니다. 스타트업 환경에 잘 맞는 지원자입니다.',
    '[{"name": "풀스택 역량", "description": "프론트/백엔드 통합 85점"}, {"name": "주도성", "description": "프로젝트 리드 88점"}, {"name": "실행 속도", "description": "빠른 실행력 80점"}, {"name": "성장 잠재력", "description": "학습 능력 75점"}]',
    NOW(6),
    NOW(6),
    NULL
);

-- ================================================
-- 채팅방 (Chat Rooms)
-- ================================================
-- 의도: 채용공고별 스터디/면접 준비 채팅방 생성
-- 근거: 컷라인 점수 기반 입장 제한 테스트
-- ================================================

-- 카카오 백엔드 - 서류 스터디방 (컷라인: 80점)
INSERT INTO chat_rooms (chat_room_id, job_master_id, created_by, room_name, max_participants, room_goal, cutline_score, preferred_conditions, status, created_at, updated_at, deleted_at)
VALUES (
    1,
    1,
    2,
    '카카오 백엔드 서류 스터디',
    5,
    'DOCUMENT',
    80,
    'Spring Boot 경험자 우대',
    'ACTIVE',
    NOW(6),
    NOW(6),
    NULL
);

-- 토스 프론트엔드 - 면접 준비방 (컷라인: 75점)
INSERT INTO chat_rooms (chat_room_id, job_master_id, created_by, room_name, max_participants, room_goal, cutline_score, preferred_conditions, status, created_at, updated_at, deleted_at)
VALUES (
    2,
    2,
    3,
    '토스 프론트 면접 준비',
    4,
    'INTERVIEW',
    75,
    'React/TypeScript 필수',
    'ACTIVE',
    NOW(6),
    NOW(6),
    NULL
);

-- 당근마켓 풀스택 - 서류 스터디방 (컷라인: 70점)
INSERT INTO chat_rooms (chat_room_id, job_master_id, created_by, room_name, max_participants, room_goal, cutline_score, preferred_conditions, status, created_at, updated_at, deleted_at)
VALUES (
    3,
    3,
    2,
    '당근 풀스택 서류 준비',
    3,
    'DOCUMENT',
    70,
    '스타트업 경험자 환영',
    'ACTIVE',
    NOW(6),
    NOW(6),
    NULL
);

-- ================================================
-- 채팅방 멤버 (Chat Room Members)
-- ================================================
-- 의도: 채팅방 참여자 및 권한 관리 테스트
-- 근거: HOST/MEMBER 권한 분리 및 강퇴 기능 검증
-- ================================================

-- Room 1 (카카오 백엔드 서류 스터디) - User 2가 방장
INSERT INTO chat_room_members (chat_room_member_id, chat_room_id, user_id, job_application_id, role, joined_at, kicked_at)
VALUES (1, 1, 2, 1, 'HOST', NOW(6), NULL);

-- Room 2 (토스 프론트엔드 면접 준비) - User 3가 방장
INSERT INTO chat_room_members (chat_room_member_id, chat_room_id, user_id, job_application_id, role, joined_at, kicked_at)
VALUES (2, 2, 3, 2, 'HOST', NOW(6), NULL);

-- Room 3 (당근마켓 풀스택 서류 준비) - User 2가 방장
INSERT INTO chat_room_members (chat_room_member_id, chat_room_id, user_id, job_application_id, role, joined_at, kicked_at)
VALUES (3, 3, 2, 3, 'HOST', NOW(6), NULL);

-- ================================================
-- 채팅 메시지 (Chat Messages)
-- ================================================
-- 의도: 채팅 기능 및 메시지 조회 테스트
-- 근거: 메시지 타입(TEXT/SYSTEM) 및 전송/삭제 로직 검증
-- ================================================

-- Room 1 - 시스템 메시지 (방 생성)
INSERT INTO chat_messages (message_id, chat_room_id, sender_id, file_id, message_type, content, sent_at, deleted_at)
VALUES (1, 1, NULL, NULL, 'SYSTEM', 'dev_kim님이 채팅방을 개설했습니다.', NOW(6), NULL);

-- Room 1 - User 2의 첫 메시지
INSERT INTO chat_messages (message_id, chat_room_id, sender_id, file_id, message_type, content, sent_at, deleted_at)
VALUES (2, 1, 2, NULL, 'TEXT', '안녕하세요! 카카오 백엔드 서류 준비 같이 하실 분들 환영합니다 :)', NOW(6), NULL);

-- Room 2 - 시스템 메시지 (방 생성)
INSERT INTO chat_messages (message_id, chat_room_id, sender_id, file_id, message_type, content, sent_at, deleted_at)
VALUES (3, 2, NULL, NULL, 'SYSTEM', 'frontend_park님이 채팅방을 개설했습니다.', NOW(6), NULL);

-- Room 2 - User 3의 첫 메시지
INSERT INTO chat_messages (message_id, chat_room_id, sender_id, file_id, message_type, content, sent_at, deleted_at)
VALUES (4, 2, 3, NULL, 'TEXT', '토스 프론트엔드 면접 준비하시는 분들 모여요!', NOW(6), NULL);

-- Room 3 - 시스템 메시지 (방 생성)
INSERT INTO chat_messages (message_id, chat_room_id, sender_id, file_id, message_type, content, sent_at, deleted_at)
VALUES (5, 3, NULL, NULL, 'SYSTEM', 'dev_kim님이 채팅방을 개설했습니다.', NOW(6), NULL);

-- Room 3 - User 2의 첫 메시지
INSERT INTO chat_messages (message_id, chat_room_id, sender_id, file_id, message_type, content, sent_at, deleted_at)
VALUES (6, 3, 2, NULL, 'TEXT', '당근마켓 풀스택 지원하시는 분들 서류 같이 준비해요!', NOW(6), NULL);
