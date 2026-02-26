# 로컬 테스트 가이드 (Postman / k6)

## 개요

카카오 OAuth 없이 로컬 환경에서 API 테스트 및 성능 테스트를 진행하기 위한 가이드입니다.

---

## 1. 사전 조건

- 로컬 서버가 `local` 프로파일로 실행 중이어야 합니다.
- DB에 V2 시드 데이터가 로딩되어 있어야 합니다 (Flyway 자동 적용).

### 서버 실행 방법

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
# 또는 IntelliJ에서 VM Options에 -Dspring.profiles.active=local 추가
```

---

## 2. JWT 토큰 발급 (카카오 로그인 대체)

**엔드포인트:** `GET /dev/token`  
**주의:** `local` 프로파일에서만 작동합니다. 운영/개발 서버에는 없는 API입니다.

### Postman 요청

```
GET http://localhost:8080/dev/token?userId=1&role=USER
```

### 응답 예시

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "userId": "1",
  "role": "USER",
  "usage": "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
}
```

### 이후 모든 API 요청에 헤더 추가

```
Authorization: Bearer {accessToken}
```

---

## 3. 시드 데이터 구조

| 항목 | ID | 설명 |
|------|-----|------|
| 유저 (HOST) | user_id=1 | nickname=test-host, 채팅방 방장 |
| 유저 (MEMBER) | user_id=2 | nickname=test-member, 채팅방 멤버 |
| 채용공고 | job_master_id=1 | 테스트주식회사 백엔드 개발자 |
| 지원 이력 | job_application_id=1(유저1), 2(유저2) | AI 평가 점수 90/85점 |
| 채팅방 | chat_room_id=1 | cutline_score=80, OPEN 상태 |
| 채팅 메시지 | message_id=1~50 | 50개 메시지 초기 세팅 |

---

## 4. 주요 테스트 API 목록

### 채팅

| 목적 | Method | URL |
|------|--------|-----|
| 채팅방 목록 조회 | GET | /api/v1/job-postings/1/chat-rooms |
| 메시지 조회 (폴링) | GET | /api/v1/chat-rooms/1/messages |
| 메시지 조회 (커서) | GET | /api/v1/chat-rooms/1/messages?cursor=50&size=20 |
| 메시지 전송 | POST | /api/v1/chat-rooms/1/messages |
| 채팅방 멤버 조회 | GET | /api/v1/chat-rooms/1/members |

### 유저

| 목적 | Method | URL |
|------|--------|-----|
| 내 정보 조회 | GET | /api/v1/users/me |
| 프로필 수정 | PATCH | /api/v1/users/me |

---

## 5. k6 성능 테스트

### 설치

```bash
brew install k6
k6 version
```

### 채팅 메시지 폴링 테스트 실행

```bash
# 프로젝트 루트에서 실행
k6 run docs/k6/load-test-chat-polling.js
```

---

## 6. 주의사항

- 시드 데이터는 Flyway 마이그레이션으로 **최초 1회**만 실행됩니다.
- DB를 초기화한 뒤 재실행하려면 `flyway_schema_history` 테이블에서 V2 레코드를 삭제하거나 DB를 드롭 후 재시작하세요.
- `/dev/token` 엔드포인트는 운영 환경에서 절대 사용 불가합니다 (`@Profile("local")` 보호).
