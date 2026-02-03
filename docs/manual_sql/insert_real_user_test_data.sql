-- ================================================
-- 실제 로그인 사용자(user_id=4) 테스트 데이터
-- ================================================
-- 사용 방법:
-- 1. 서버 기동 (V100~V104 자동 실행)
-- 2. 카카오 로그인 → user_id 확인
-- 3. DataGrip에서 이 SQL 수동 실행
-- ================================================
-- 의도: 로컬 환경에서 실제 OAuth 로그인한 계정으로 전체 플로우 테스트
-- 근거: AI API 없이도 지원/평가/채팅방 기능 테스트 가능
-- ================================================

-- ⚠️ 주의: user_id가 4가 아니면 모든 '4'를 실제 user_id로 변경하세요!

-- ================================================
-- 1. 지원서 제출 (User 4가 3개 공고에 지원)
-- ================================================
INSERT INTO job_applications (job_application_id, user_id, job_master_id, status, created_at, updated_at, deleted_at)
VALUES 
    (4, 4, 1, 'SUBMITTED', NOW(6), NOW(6), NULL),
    (5, 4, 2, 'SUBMITTED', NOW(6), NOW(6), NULL),
    (6, 4, 3, 'SUBMITTED', NOW(6), NOW(6), NULL);

-- ================================================
-- 2. AI 이력서 분석 결과
-- ================================================
INSERT INTO ai_resume_analysis (
    analysis_id, job_application_id, career_summary, key_achievements, 
    skill_match_score, experience_relevance, growth_trajectory, 
    created_at, updated_at, deleted_at
)
VALUES 
    (4, 4, '백엔드 개발 3년차, Spring Boot 기반 마이크로서비스 아키텍처 구축 경험',
     '["대규모 트래픽 처리 시스템 설계", "API 응답 속도 40% 개선", "Jenkins CI/CD 파이프라인 구축"]',
     88, '카카오가 요구하는 백엔드 역량과 높은 일치도를 보임. Spring Boot/JPA 실무 경험 풍부.',
     '주니어 개발자에서 시니어로 성장하는 과정이 명확하며, 기술 깊이가 점진적으로 향상됨.',
     NOW(6), NOW(6), NULL),
    
    (5, 5, 'React/TypeScript 기반 프론트엔드 개발 2년차',
     '["성능 최적화로 로딩 속도 50% 개선", "재사용 가능한 컴포넌트 라이브러리 구축", "Next.js SSR 적용"]',
     75, 'React 생태계에 대한 이해도는 우수하나, 토스가 요구하는 디자인 시스템 경험은 다소 부족.',
     '프론트엔드 기술 스택을 빠르게 습득하는 학습 능력이 돋보임.',
     NOW(6), NOW(6), NULL),
    
    (6, 6, '풀스택 개발자, 스타트업 환경에서 0→1 서비스 런칭 경험',
     '["MVP 개발부터 배포까지 단독 진행", "사용자 피드백 기반 빠른 이터레이션", "AWS 인프라 구축 및 모니터링"]',
     82, '스타트업 환경에서의 빠른 실행력과 주도성이 당근마켓의 문화와 잘 맞음.',
     '기술적 깊이보다는 빠른 실행력과 문제 해결 능력에 강점을 보임.',
     NOW(6), NOW(6), NULL);

-- ================================================
-- 3. AI 포트폴리오 분석 결과
-- ================================================
INSERT INTO ai_portfolio_analysis (
    analysis_id, job_application_id, project_quality_score, technical_depth,
    innovation_level, code_quality_assessment, project_highlights,
    created_at, updated_at, deleted_at
)
VALUES 
    (4, 4, 85,
     '마이크로서비스 아키텍처, Redis 캐싱, Kafka 메시징 시스템 등 현대적 백엔드 기술 스택 활용',
     '트래픽 급증 시 자동 스케일링 전략 도입, 장애 대응 시나리오 문서화',
     'Clean Code 원칙 준수, 단위 테스트 커버리지 80% 이상, API 문서 자동화',
     '["E-커머스 주문 시스템 설계", "실시간 알림 서비스 구축", "대용량 데이터 처리 파이프라인"]',
     NOW(6), NOW(6), NULL),
    
    (5, 5, 78,
     'React Hooks, Context API, React Query 등 최신 React 패턴 적용',
     '성능 최적화를 위한 Lazy Loading, Code Splitting 전략 구현',
     'ESLint/Prettier 설정, 컴포넌트 단위 테스트, Storybook 문서화',
     '["관리자 대시보드 개발", "실시간 차트 시각화", "반응형 디자인 구현"]',
     NOW(6), NOW(6), NULL),
    
    (6, 6, 80,
     '백엔드(Spring Boot) + 프론트엔드(React) 풀스택 개발, Docker 컨테이너화',
     '빠른 MVP 개발, A/B 테스트 구현, 사용자 행동 분석 대시보드 구축',
     '모노레포 구조, Git 브랜치 전략, CI/CD 파이프라인 자동화',
     '["SaaS 플랫폼 MVP 개발", "결제 시스템 통합", "사용자 분석 툴 구축"]',
     NOW(6), NOW(6), NULL);

-- ================================================
-- 4. AI 최종 평가 결과 (overall_score 포함)
-- ================================================
INSERT INTO ai_applicant_evaluation (
    evaluation_id, job_application_id, overall_score, one_line_review,
    feedback_detail, comparison_scores, created_at, updated_at, deleted_at
)
VALUES 
    (4, 4, 87, '백엔드 기술 스택과 아키텍처 이해도가 매우 우수한 지원자입니다.',
     '마이크로서비스 아키텍처 설계 경험과 대규모 트래픽 처리 능력이 돋보입니다. 카카오의 기술 문화와 잘 맞을 것으로 예상됩니다.',
     '[{"name": "기술 역량", "description": "Java/Spring 생태계 숙련도 90점"}, {"name": "문제 해결 능력", "description": "시스템 설계 및 최적화 88점"}]',
     NOW(6), NOW(6), NULL),
    
    (5, 5, 76, 'React 기술 스택은 우수하나, 디자인 시스템 경험을 보완하면 좋겠습니다.',
     'React/TypeScript 기반 개발 능력은 검증되었으나, 토스가 중요시하는 디자인 시스템 구축 경험이 부족합니다.',
     '[{"name": "기술 역량", "description": "React/TypeScript 숙련도 78점"}, {"name": "UI/UX 감각", "description": "디자인 감각 70점"}]',
     NOW(6), NOW(6), NULL),
    
    (6, 6, 83, '빠른 실행력과 주도성을 갖춘 스타트업 적합 개발자입니다.',
     '기획부터 배포까지 전 과정을 주도적으로 이끈 경험이 풍부합니다. 당근마켓의 빠른 개발 문화와 잘 맞을 것으로 예상됩니다.',
     '[{"name": "풀스택 역량", "description": "프론트/백엔드 통합 개발 85점"}, {"name": "주도성", "description": "프로젝트 리드 경험 88점"}]',
     NOW(6), NOW(6), NULL);

-- ================================================
-- 5. 채팅방 생성 (User 4가 방장)
-- ================================================
INSERT INTO chat_rooms (
    chat_room_id, job_master_id, created_by, room_name, max_participants,
    room_goal, cutline_score, preferred_conditions, status,
    created_at, updated_at, deleted_at
)
VALUES 
    (4, 1, 4, '카카오 백엔드 면접 스터디 (실전 모의면접)', 6,
     'INTERVIEW', 85, '시스템 디자인 면접 대비, Spring 생태계 질문 준비', 'ACTIVE',
     NOW(6), NOW(6), NULL),
    
    (5, 3, 4, '당근 풀스택 합격 전략 공유방', 5,
     'DOCUMENT', 80, '풀스택 포트폴리오 피드백, 이력서 첨삭', 'ACTIVE',
     NOW(6), NOW(6), NULL);

-- ================================================
-- 6. 채팅방 멤버 추가
-- ================================================
INSERT INTO chat_room_members (
    chat_room_member_id, chat_room_id, user_id, job_application_id,
    role, joined_at, kicked_at
)
VALUES 
    (4, 4, 4, 4, 'HOST', NOW(6), NULL),
    (5, 4, 2, 1, 'MEMBER', NOW(6), NULL),
    (6, 5, 4, 6, 'HOST', NOW(6), NULL),
    (7, 5, 2, 3, 'MEMBER', NOW(6), NULL);

-- ================================================
-- 7. 샘플 채팅 메시지
-- ================================================
INSERT INTO chat_messages (
    chat_message_id, chat_room_id, sender_id, message_type, content,
    sent_at, deleted_at
)
VALUES 
    (4, 4, 4, 'TEXT', '안녕하세요! 카카오 백엔드 면접 준비 같이 하실 분 환영합니다 👋', NOW(6), NULL),
    (5, 4, 2, 'TEXT', '반갑습니다! Spring Boot 관련 질문 많이 받나요?', NOW(6), NULL),
    (6, 4, 4, 'TEXT', '네, 특히 JPA N+1 문제 해결 방법이랑 트랜잭션 격리 수준에 대해 많이 물어보더라구요', NOW(6), NULL),
    (7, 5, 4, 'TEXT', '당근마켓 지원하시는 분들 모여주세요! 포트폴리오 피드백 같이 해요 🥕', NOW(6), NULL),
    (8, 5, 2, 'TEXT', '저도 지원했어요! MVP 개발 경험 어필하는게 중요한가요?', NOW(6), NULL),
    (9, 5, 4, 'TEXT', '네 맞아요! 빠르게 실행하고 사용자 피드백 반영한 사례 있으면 좋아요', NOW(6), NULL);

-- ================================================
-- 완료!
-- ================================================
SELECT '✅ 실제 사용자 테스트 데이터 삽입 완료!' as message;
