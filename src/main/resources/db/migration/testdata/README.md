# 테스트 데이터 마이그레이션 (로컬 환경 전용)

## 📁 파일 구조

```
src/main/resources/db/migration/
├── V1__init.sql                           # 스키마 정의 (공통)
└── testdata/                              # 로컬 전용 테스트 데이터
    ├── V100__insert_test_users.sql        # 테스트 사용자 3명
    ├── V101__insert_test_companies.sql    # 테스트 기업 데이터
    ├── V102__insert_test_skills.sql       # 기술 스택
    ├── V103__insert_test_job_masters.sql  # 채용공고
    └── V104__insert_test_chat_rooms.sql   # 채팅방 및 메시지
```

## 🎯 설계 의도

### 1. 환경별 분리
- **로컬**: `testdata` 디렉토리 포함 (자동 테스트 데이터 생성)
- **dev/prod**: `testdata` 제외 (스키마만 적용)

### 2. 버전 관리
- 100번대 버전: 테스트 데이터 전용
- 스키마 변경(V1, V2...)과 명확히 구분

### 3. 데이터 일관성
- 모든 개발자가 동일한 테스트 데이터 사용
- FK 관계가 올바르게 설정된 정합성 있는 데이터

## 👥 테스트 사용자 (3명)

| user_id | nickname | role | email | OAuth Provider |
|---------|----------|------|-------|----------------|
| 1 | admin_thunder | ADMIN | admin@thunder11.com | KAKAO |
| 2 | dev_kim | USER | dev.kim@example.com | KAKAO |
| 3 | frontend_park | USER | frontend.park@example.com | KAKAO |

## 🏢 테스트 기업 (3개)

1. **카카오** - 백엔드 개발자 (Java, Spring Boot, JPA, MySQL, Docker, AWS)
2. **토스** - 프론트엔드 개발자 (TypeScript, React, Next.js)
3. **당근마켓** - 풀스택 개발자 (Java, Spring Boot, TypeScript, React, Docker)

## 💬 테스트 채팅방 (3개)

| 채팅방 | 공고 | 방장 | 컷라인 점수 | 상태 |
|-------|------|------|------------|------|
| 카카오 백엔드 서류 스터디 | 카카오 백엔드 | dev_kim | 80점 | ACTIVE |
| 토스 프론트 면접 준비 | 토스 프론트엔드 | frontend_park | 75점 | ACTIVE |
| 당근 풀스택 서류 준비 | 당근마켓 풀스택 | dev_kim | 70점 | ACTIVE |

## 🚀 사용 방법

### 1. 서버 첫 실행 (자동 적용)
```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```
→ Flyway가 자동으로 스키마 + 테스트 데이터 생성

### 2. DB 초기화 (수동)
```bash
# 기존 데이터 모두 삭제 + 테스트 데이터 재생성
./gradlew flywayCleanMigrateLocal

# 또는 개별 실행
./gradlew flywayClean  # DB 초기화
./gradlew flywayMigrate  # 마이그레이션 실행
```

### 3. 마이그레이션 상태 확인
```bash
./gradlew flywayInfo
```

## ⚙️ 설정 (application-local.yml)

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate  # Flyway가 스키마 관리
  
  flyway:
    enabled: true
    clean-disabled: false  # clean 명령 허용
    locations:
      - classpath:db/migration
      - classpath:db/migration/testdata  # 로컬에서만
```

## 📌 주의사항

### ✅ 해야 할 것
- 로컬에서 테스트 시 이 데이터 활용
- 새로운 테스트 데이터 필요 시 V105, V106... 추가
- FK 관계 유지하며 데이터 삽입

### ❌ 하지 말아야 할 것
- dev/prod 환경에 testdata 적용
- 운영 데이터와 혼용
- AUTO_INCREMENT 값 임의 변경

## 🔄 데이터 재생성 시나리오

### 시나리오 1: 깨끗한 초기화
```bash
./gradlew flywayCleanMigrateLocal
./gradlew bootRun --args='--spring.profiles.active=local'
```

### 시나리오 2: 스키마 변경 후 재적용
1. V1__init.sql 수정
2. `./gradlew flywayCleanMigrateLocal`
3. 서버 재시작

### 시나리오 3: 테스트 데이터만 변경
1. V100~V104 파일 수정
2. `./gradlew flywayCleanMigrateLocal`
3. 서버 재시작

## 📊 마이그레이션 히스토리

Flyway는 `flyway_schema_history` 테이블에 실행 이력 저장:

```sql
SELECT * FROM flyway_schema_history ORDER BY installed_rank;
```

| installed_rank | version | description | script | installed_on | success |
|---------------|---------|-------------|--------|--------------|---------|
| 1 | 1 | init | V1__init.sql | 2026-02-03 | 1 |
| 2 | 100 | insert test users | V100__insert_test_users.sql | 2026-02-03 | 1 |
| 3 | 101 | insert test companies | V101__insert_test_companies.sql | 2026-02-03 | 1 |
| ... | ... | ... | ... | ... | ... |

## 🐛 문제 해결

### 문제: Flyway 마이그레이션 실패
```bash
# 에러 확인
./gradlew flywayInfo

# 강제 초기화
./gradlew flywayClean flywayMigrate
```

### 문제: 테스트 데이터가 적용되지 않음
→ `application-local.yml`에서 `testdata` 경로 확인

### 문제: FK 제약조건 에러
→ 데이터 삽입 순서 확인 (users → companies → job_masters → ...)
