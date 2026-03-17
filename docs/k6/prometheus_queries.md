# Prometheus 지표 수집 가이드

> URL: http://localhost:9101/actuator/prometheus
> k6 테스트 실행 중 / 완료 직후 아래 쿼리를 Prometheus UI (localhost:9090) 에서 실행

---

## 핵심 지표 1 — Tomcat 활성 스레드 수 (이번 개선의 핵심)

```
tomcat_threads_busy_threads{application="scuad"}
```

**의도**: AI 동기 호출 구간에서 Tomcat HTTP 스레드가 얼마나 점유되는지 확인.
VU 10명이 동시 요청 시 busy_threads가 10 근처까지 치솟으면 문제 재현 성공.

초안에 있던 `hikaricp_connections_active` 대신 이 지표를 사용하는 이유:
AI WebClient.block() 동안에는 DB 커넥션을 잡지 않으므로
HikariCP 지표에는 문제가 드러나지 않음. Tomcat 스레드 지표가 핵심.

---

## 핵심 지표 2 — JVM 전체 라이브 스레드 (초안 유지)

```
jvm_threads_live_threads{application="scuad"}
```

---

## 참고 지표 3 — HTTP 요청 응답시간 분포 (서버 관점 p95)

```
histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket{uri=~".*comparison.*", application="scuad"}[1m])) by (le))
```

---

## 스크린샷 타이밍

| 시점 | 저장 파일명 |
|---|---|
| 동기 VU 10명 테스트 중 피크 | `sync_vu10_prometheus.png` |
| 동기 VU 50명 테스트 중 피크 | `sync_vu50_prometheus.png` |
| 비동기 VU 10명 테스트 중 피크 | `async_vu10_prometheus.png` |
| 비동기 VU 50명 테스트 중 피크 | `async_vu50_prometheus.png` |

---

## 기록할 수치 (개선 문서 표에 채울 항목)

| 메트릭 | 동기 VU 10명 | 동기 VU 50명 | 비동기 VU 10명 | 비동기 VU 50명 |
|---|---|---|---|---|
| tomcat_threads_busy_threads (피크) | - | - | - | - |
| jvm_threads_live_threads (피크) | - | - | - | - |
