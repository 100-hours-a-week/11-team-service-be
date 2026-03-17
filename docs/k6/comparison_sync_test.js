/**
 * [동기 호출 k6 부하 테스트]
 *
 * 목적: RabbitMQ 비동기 전환 전 기준선(baseline) 측정
 *       - AI 동기 호출로 인한 Tomcat 스레드 점유 현상을 수치로 확인
 *       - Thread.sleep(3000) 이 AiServiceClient에 적용된 상태에서 실행
 *
 * 실행 방법:
 *   1. VU 10명 테스트:  k6 run --vus 10 --duration 30s docs/k6/comparison_sync_test.js
 *   2. VU 50명 테스트:  k6 run --vus 50 --duration 30s docs/k6/comparison_sync_test.js
 *   ※ 토큰은 setup() 에서 /api/test/token 을 자동 호출하여 발급됨 (수동 입력 불필요)
 *
 * 주의사항:
 *   - 테스트 전 ai_applicant_comparison 테이블을 비워야 AI 호출이 매번 발생함
 *     → DELETE FROM ai_applicant_comparison;  (MySQL 직접 실행)
 *   - Prometheus 지표는 http://localhost:9090 에서 수집
 */

import http from "k6/http";
import { check, sleep } from "k6";
import { Trend, Rate } from "k6/metrics";

// ── 설정 ─────────────────────────────────────────────────────────────
const BASE_URL = "http://localhost:8080";
const CHAT_ROOM_ID = 1;       // V2 시드 데이터 기준
const TARGET_MEMBER_ID = 2;   // V2 시드 데이터 기준 (MEMBER)
const TOKEN_USER_ID = 1;      // V2 시드 데이터 기준 (HOST)
// ─────────────────────────────────────────────────────────────────────

// 커스텀 지표
const responseTimeTrend = new Trend("comparison_response_time");
const errorRate = new Rate("comparison_error_rate");

export const options = {
  thresholds: {
    // 임계값은 측정용이 아닌 기록용 — 동기에서 초과해도 무방
    "comparison_response_time": ["p(95)<60000"],
    "comparison_error_rate": ["rate<0.5"],
  },
};

// setup(): 테스트 시작 전 1회 실행 — 토큰 자동 발급
// 반환값은 default 함수의 data 파라미터로 전달됨
export function setup() {
  const res = http.get(`${BASE_URL}/api/test/token?userId=${TOKEN_USER_ID}&role=USER`);

  check(res, { "토큰 발급 성공": (r) => r.status === 200 });

  const token = res.json("accessToken");
  if (!token) {
    throw new Error("토큰 발급 실패 — 서버가 실행 중인지, local 프로필인지 확인하세요.");
  }

  console.log(`✅ 토큰 자동 발급 완료 (userId=${TOKEN_USER_ID})`);
  return { token };
}

export default function (data) {
  const url = `${BASE_URL}/api/v1/chat-rooms/${CHAT_ROOM_ID}/members/${TARGET_MEMBER_ID}/comparison`;

  const params = {
    headers: {
      Authorization: `Bearer ${data.token}`,
      "Content-Type": "application/json",
    },
    timeout: "60s",  // Thread.sleep(3000) + DB 트랜잭션 대기 시간 고려하여 넉넉하게 설정
  };

  const res = http.get(url, params);

  // 검증
  // 의도: 스레드 블로킹 현상은 응답시간으로 측정.
  //       VU 10명이 동일한 comparison을 동시에 요청하면 UNIQUE 제약 충돌로 500이 발생하지만,
  //       이는 테스트 시나리오 특성(같은 두 사람 동시 비교)에서 비롯된 것이며,
  //       핵심 목표(스레드 점유로 인한 응답 지연)는 응답시간 수치로 이미 확인 가능.
  //       따라서 응답을 받은 경우(200 또는 500) 모두 "요청 처리됨"으로 간주하고 응답시간 기록.
  const responded = check(res, {
    "서버 응답 받음 (200 or 500)": (r) => r.status === 200 || r.status === 500,
    "타임아웃 없음": (r) => r.timings.duration < 60000,
  });

  // 네트워크 레벨 실패(타임아웃, connection refused)만 에러로 집계
  const networkError = res.status === 0;
  if (networkError && __ITER === 0 && __VU === 1) {
    console.error(`❌ 네트워크 오류 - status: ${res.status}, body: ${res.body}`);
  }

  responseTimeTrend.add(res.timings.duration);
  errorRate.add(networkError);

  // VU 간 간격 없음 — 스레드 점유 극대화 목적
  sleep(0.1);
}

export function handleSummary(data) {
  const vus = __ENV.K6_VUS || "unknown";

  return {
    stdout: `
=======================================================
  [동기 호출 k6 결과 요약] VU: ${vus}명
=======================================================
  avg 응답시간  : ${Math.round(data.metrics.comparison_response_time?.values?.avg || 0)} ms
  p95 응답시간  : ${Math.round(data.metrics.comparison_response_time?.values["p(95)"] || 0)} ms
  p99 응답시간  : ${Math.round(data.metrics.comparison_response_time?.values["p(99)"] || 0)} ms
  최대 응답시간 : ${Math.round(data.metrics.comparison_response_time?.values?.max || 0)} ms
  요청 성공률   : ${(100 - (data.metrics.comparison_error_rate?.values?.rate || 0) * 100).toFixed(1)} %
  총 요청 수    : ${data.metrics.http_reqs?.values?.count || 0}
=======================================================
→ 이 수치를 개선 문서 "제2 명분" 표에 기록하세요.
`,
    [`docs/k6/results/comparison_sync_vu${vus}_result.json`]: JSON.stringify(data, null, 2),
  };
}
