-- ================================================
-- 테스트 사용자 데이터 (로컬 환경 전용)
-- ================================================
-- 의도: 로컬 개발/테스트 시 즉시 사용 가능한 사용자 계정 제공
-- 근거: 매번 회원가입 없이 인증/인가, 채용공고, 채팅 기능 테스트 가능
-- ================================================

-- User 1: ADMIN (관리자)
INSERT INTO users (user_id, profile_image_file_id, role, nickname, status, created_at, updated_at, deleted_at)
VALUES (1, NULL, 'ADMIN', 'admin_thunder', 'ACTIVE', NOW(6), NOW(6), NULL);

-- User 2: USER (일반 사용자 1 - 백엔드 개발자)
INSERT INTO users (user_id, profile_image_file_id, role, nickname, status, created_at, updated_at, deleted_at)
VALUES (2, NULL, 'USER', 'dev_kim', 'ACTIVE', NOW(6), NOW(6), NULL);

-- User 3: USER (일반 사용자 2 - 프론트엔드 개발자)
INSERT INTO users (user_id, profile_image_file_id, role, nickname, status, created_at, updated_at, deleted_at)
VALUES (3, NULL, 'USER', 'frontend_park', 'ACTIVE', NOW(6), NOW(6), NULL);

-- ================================================
-- OAuth 계정 연동 (카카오)
-- ================================================
-- 의도: 카카오 OAuth 로그인 시나리오 테스트
-- 근거: 실제 카카오 로그인 없이 OAuth 연동 상태 재현
-- ================================================

-- Admin OAuth
INSERT INTO user_oauth_accounts (oauth_account_id, user_id, email, provider, provider_user_id, provider_email, connected_at, created_at, updated_at, deleted_at)
VALUES (1, 1, 'admin@thunder11.com', 'KAKAO', 'kakao_1234567890', 'admin@thunder11.com', NOW(6), NOW(6), NOW(6), NULL);

-- User 2 OAuth
INSERT INTO user_oauth_accounts (oauth_account_id, user_id, email, provider, provider_user_id, provider_email, connected_at, created_at, updated_at, deleted_at)
VALUES (2, 2, 'dev.kim@example.com', 'KAKAO', 'kakao_2345678901', 'dev.kim@example.com', NOW(6), NOW(6), NOW(6), NULL);

-- User 3 OAuth
INSERT INTO user_oauth_accounts (oauth_account_id, user_id, email, provider, provider_user_id, provider_email, connected_at, created_at, updated_at, deleted_at)
VALUES (3, 3, 'frontend.park@example.com', 'KAKAO', 'kakao_3456789012', 'frontend.park@example.com', NOW(6), NOW(6), NOW(6), NULL);
