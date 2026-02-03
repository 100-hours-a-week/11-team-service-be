# 실제 사용자 테스트 데이터 설정 가이드

## 📋 사용 시나리오

로컬 환경에서 **실제 OAuth 로그인한 계정**으로 전체 플로우를 테스트하기 위한 가이드입니다.

---

## 🔄 실행 순서

### 1단계: 기본 테스트 데이터 생성

```bash
# DB 초기화 (DataGrip)
SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS flyway_schema_history;
-- (모든 테이블 삭제)
SET FOREIGN_KEY_CHECKS = 1;

# 서버 시작
./gradlew bootRun --args='--spring.profiles.active=local'
```

→ V100~V104 자동 실행 (더미 사용자 3명, 채용공고 3개, 채팅방 3개)

---

### 2단계: 실제 OAuth 로그인

브라우저에서 카카오 로그인 진행:
```
http://localhost:8080/oauth2/authorization/kakao
```

로그인 후 DataGrip에서 user_id 확인:
```sql
SELECT user_id, nickname, role, status 
FROM users 
ORDER BY created_at DESC 
LIMIT 1;
```

예상 결과: `user_id = 4` (또는 다른 번호)

---

### 3단계: 실제 사용자 테스트 데이터 추가

**❗ 중요: user_id가 4가 아닌 경우**

`manual_insert_real_user_test_data.sql` 파일을 열어서 모든 `4`를 실제 user_id로 변경:

```sql
-- 예: user_id가 5인 경우
-- 변경 전
INSERT INTO job_applications (job_application_id, user_id, ...)
VALUES (4, 4, 1, ...);

-- 변경 후
INSERT INTO job_applications (job_application_id, user_id, ...)
VALUES (4, 5, 1, ...);
```

**DataGrip에서 SQL 실행:**
```sql
-- manual_insert_real_user_test_data.sql 전체 실행
```

---

## ✅ 생성되는 테스트 데이터

### 지원 내역 (3개)
- 카카오 백엔드 (job_application_id=4, 점수 87) ✅ 채팅방 입장 가능
- 토스 프론트엔드 (job_application_id=5, 점수 76) ⚠️ 컷라인 미달
- 당근마켓 풀스택 (job_application_id=6, 점수 83) ✅ 채팅방 입장 가능

### AI 분석 결과
- 이력서 분석 (ai_resume_analysis) - 경력/기술/성장 분석
- 포트폴리오 분석 (ai_portfolio_analysis) - 프로젝트 퀄리티/기술 깊이
- 최종 평가 (ai_applicant_evaluation) - 종합 점수 및 피드백

### 채팅방 (2개 방장)
- 카카오 백엔드 면접 준비방 (컷라인 85점, 정원 6명)
  - User 4 (HOST) + User 2 (MEMBER)
- 당근마켓 풀스택 서류 준비방 (컷라인 80점, 정원 5명)
  - User 4 (HOST) + User 2 (MEMBER)

### 채팅 메시지
- 각 채팅방별 샘플 메시지 3개씩

---

## 🧪 테스트 시나리오

### 시나리오 1: 내 지원 내역 조회
```bash
GET /api/v1/applications
Authorization: Bearer {access_token}
```

**기대 결과:**
```json
[
  {
    "jobApplicationId": 4,
    "jobTitle": "백엔드 개발자",
    "companyName": "카카오",
    "status": "SUBMITTED",
    "overallScore": 87
  },
  {
    "jobApplicationId": 5,
    "jobTitle": "프론트엔드 개발자",
    "companyName": "토스",
    "status": "SUBMITTED",
    "overallScore": 76
  },
  {
    "jobApplicationId": 6,
    "jobTitle": "풀스택 개발자",
    "companyName": "당근마켓",
    "status": "SUBMITTED",
    "overallScore": 83
  }
]
```

---

### 시나리오 2: 내가 만든 채팅방 조회
```bash
GET /api/v1/chat/rooms?createdBy=4
Authorization: Bearer {access_token}
```

**기대 결과:**
```json
[
  {
    "chatRoomId": 4,
    "roomName": "카카오 백엔드 면접 스터디 (실전 모의면접)",
    "jobTitle": "백엔드 개발자",
    "companyName": "카카오",
    "cutlineScore": 85,
    "currentParticipants": 2,
    "maxParticipants": 6
  },
  {
    "chatRoomId": 5,
    "roomName": "당근 풀스택 합격 전략 공유방",
    "jobTitle": "풀스택 개발자",
    "companyName": "당근마켓",
    "cutlineScore": 80,
    "currentParticipants": 2,
    "maxParticipants": 5
  }
]
```

---

### 시나리오 3: 채팅 메시지 조회
```bash
GET /api/v1/chat/rooms/4/messages
Authorization: Bearer {access_token}
```

**기대 결과:**
```json
[
  {
    "messageId": 4,
    "senderNickname": "남법준",
    "content": "안녕하세요! 카카오 백엔드 면접 준비 같이 하실 분 환영합니다 👋",
    "sentAt": "2026-02-03T13:50:00"
  },
  {
    "messageId": 5,
    "senderNickname": "dev_kim",
    "content": "반갑습니다! Spring Boot 관련 질문 많이 받나요?",
    "sentAt": "2026-02-03T13:51:00"
  },
  {
    "messageId": 6,
    "senderNickname": "남법준",
    "content": "네, 특히 JPA N+1 문제 해결 방법이랑 트랜잭션 격리 수준에 대해 많이 물어보더라구요",
    "sentAt": "2026-02-03T13:52:00"
  }
]
```

---

### 시나리오 4: AI 평가 결과 조회
```bash
GET /api/v1/applications/4/evaluation
Authorization: Bearer {access_token}
```

**기대 결과:**
```json
{
  "evaluationId": 4,
  "overallScore": 87,
  "oneLineReview": "백엔드 기술 스택과 아키텍처 이해도가 매우 우수한 지원자입니다.",
  "feedbackDetail": "마이크로서비스 아키텍처 설계 경험과 대규모 트래픽 처리 능력이 돋보입니다...",
  "comparisonScores": [
    {"name": "기술 역량", "description": "Java/Spring 생태계 숙련도 90점"},
    {"name": "문제 해결 능력", "description": "시스템 설계 및 최적화 88점"},
    {"name": "커뮤니케이션", "description": "기술 문서화 능력 85점"},
    {"name": "경력", "description": "관련 실무 경험 85점"}
  ]
}
```

---

## 🔧 트러블슈팅

### Q1: user_id가 4가 아니에요!

**A:** `manual_insert_real_user_test_data.sql` 파일에서 모든 `4`를 실제 user_id로 변경하세요.

```bash
# Mac/Linux
sed -i '' 's/, 4,/, 5,/g' manual_insert_real_user_test_data.sql

# 또는 IntelliJ에서 Find & Replace
# 찾기: , 4,
# 바꾸기: , 5,
```

---

### Q2: 외래키 제약 조건 위반 에러!

**A:** user_id=4가 존재하는지 확인하세요.

```sql
SELECT * FROM users WHERE user_id = 4;
```

없으면 → 먼저 OAuth 로그인 진행!

---

### Q3: 채팅방에 입장할 수 없어요!

**A:** 점수 확인하세요.

```sql
SELECT 
    ja.job_application_id,
    jm.job_title,
    ae.overall_score,
    cr.cutline_score,
    CASE 
        WHEN ae.overall_score >= cr.cutline_score THEN '입장 가능 ✅'
        ELSE '점수 부족 ❌'
    END as status
FROM job_applications ja
JOIN ai_applicant_evaluation ae ON ja.job_application_id = ae.job_application_id
JOIN job_masters jm ON ja.job_master_id = jm.job_master_id
JOIN chat_rooms cr ON jm.job_master_id = cr.job_master_id
WHERE ja.user_id = 4;
```

---

## 📝 정리

| 단계 | 작업 | 도구 |
|------|------|------|
| 1 | DB 초기화 + 서버 시작 | Terminal + DataGrip |
| 2 | OAuth 로그인 | Browser |
| 3 | user_id 확인 | DataGrip |
| 4 | manual SQL 실행 | DataGrip |
| 5 | API 테스트 | Postman/Insomnia |

---

이제 실제 계정으로 전체 플로우를 테스트할 수 있습니다! 🚀
