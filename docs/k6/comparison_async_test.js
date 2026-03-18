/**
 * [비동기 전환 k6 부하 테스트]
 *
 * 목적: RabbitMQ 비동기 전환 후 스레드 점유 개선 여부를 수치로 확인
 *       - POST 요청 → 202 즉시 반환 → 스레드 블로킹 없음을 검증
 *
 * 실행 방법:
 *   1. VU 10명: k6 run --vus 10 --duration 60s --env K6_VUS=10 docs/k6/comparison_async_test.js
 *   2. VU 50명: k6 run --vus 50 --duration 60s --env K6_VUS=50 docs/k6/comparison_async_test.js
 *   ※ 토큰은 setup()에서 /api/test/token 자동 발급
 */

import http from "k6/http";
import { check, sleep } from "k6";
import { Trend, Rate } from "k6/metrics";

// ── 설정 ─────────────────────────────────────────────────────────────
const BASE_URL = "http://localhost:8080";
const CHAT_ROOM_ID = 1;
const TARGET_MEMBER_ID = 2;
const TOKEN_USER_ID = 1;
// ─────────────────────────────────────────────────────────────────────

const responseTimeTrend = new Trend("comparison_response_time");
const errorRate = new Rate("comparison_error_rate");

export const options = {
  thresholds: {
    "comparison_response_time": ["p(95)<3000"],  // 비동기 전환 후 MQ publish 시간만 포함되어야 함
    "comparison_error_rate": ["rate<0.1"],
  },
};

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
    timeout: "10s",  // 비동기 전환 후 즉시 반환되므로 타임아웃을 짧게 설정
  };

  // 비동기 전환 후 POST 엔드포인트로 변경
  const res = http.post(url, null, params);

  const ok = check(res, {
    "status is 202": (r) => r.status === 202,
  });

  if (!ok && __ITER === 0 && __VU === 1) {
    console.error(`❌ 요청 실패 - status: ${res.status}, body: ${res.body}`);
  }

  responseTimeTrend.add(res.timings.duration);
  errorRate.add(!ok);

  sleep(0.1);
}

export function handleSummary(data) {
  const vus = __ENV.K6_VUS || "unknown";

  return {
    stdout: `
=======================================================
  [비동기 전환 k6 결과 요약] VU: ${vus}명
=======================================================
  avg 응답시간  : ${Math.round(data.metrics.comparison_response_time?.values?.avg || 0)} ms
  p95 응답시간  : ${Math.round(data.metrics.comparison_response_time?.values["p(95)"] || 0)} ms
  p99 응답시간  : ${Math.round(data.metrics.comparison_response_time?.values["p(99)"] || 0)} ms
  최대 응답시간 : ${Math.round(data.metrics.comparison_response_time?.values?.max || 0)} ms
  요청 성공률   : ${(100 - (data.metrics.comparison_error_rate?.values?.rate || 0) * 100).toFixed(1)} %
  총 요청 수    : ${data.metrics.http_reqs?.values?.count || 0}
=======================================================
→ 이 수치를 개선 문서 "제2 명분" 비동기 전환 열에 기록하세요.
`,
    [`docs/k6/results/comparison_async_vu${vus}_result.json`]: JSON.stringify(data, null, 2),
  };
}
