/**
 * 폴링 방식 베이스라인 측정 - VU 100명
 *
 * 목적: 유저 수 증가 시 응답 성능이 함께 악화되는 실제 성능 저하 확인
 *       (VU 10명 대비 p95가 얼마나 증가하는지 측정)
 *
 * 조건:
 *   - VU: 100명
 *   - Duration: 30s
 *   - 폴링 주기: 2초 (실제 클라이언트 주기와 동일)
 *   - 대상 API: GET /api/v1/chat-rooms/1/messages
 *
 * 실행:
 *   k6 run k6/polling_vu100.js
 *
 * 결과 파일:
 *   k6/polling_vu100_result.json
 */
import http from 'k6/http';
import { check, sleep } from 'k6';
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.0.1/index.js';

export const options = {
    vus: 100,
    duration: '30s',
    thresholds: {
        http_req_failed: ['rate<0.01'],
        http_req_duration: ['p(95)<500'],
    },
};

// setup(): VU 시작 전 1회 실행 — 토큰 자동 발급
export function setup() {
    const tokenRes = http.get('http://localhost:8080/api/test/token?userId=1&role=USER');
    const body = JSON.parse(tokenRes.body);
    const token = body.data.accessToken;
    console.log(`[setup] 토큰 발급 완료: ${token.substring(0, 20)}...`);
    return { token };
}

// default(): 각 VU가 반복 실행하는 메인 함수
export default function ({ token }) {
    const headers = {
        Authorization: `Bearer ${token}`,
        'Content-Type': 'application/json',
    };

    // 폴링 대상 API: 채팅방 1번의 메시지 목록 조회
    const res = http.get('http://localhost:8080/api/v1/chat-rooms/1/messages', { headers });

    check(res, {
        'status is 200': (r) => r.status === 200,
        'response has data': (r) => {
            try {
                const body = JSON.parse(r.body);
                return body.data !== undefined;
            } catch {
                return false;
            }
        },
    });

    // 폴링 주기: 2초
    sleep(2);
}

// handleSummary(): 테스트 종료 후 결과 출력 및 저장
export function handleSummary(data) {
    const dur = data.metrics.http_req_duration.values;
    const reqs = data.metrics.http_reqs.values;

    console.log('\n========== 폴링 VU 100명 결과 요약 ==========');
    console.log(`  avg 응답 시간 : ${dur.avg.toFixed(2)}ms`);
    console.log(`  p90 응답 시간 : ${dur['p(90)'].toFixed(2)}ms`);
    console.log(`  p95 응답 시간 : ${dur['p(95)'].toFixed(2)}ms`);
    console.log(`  max 응답 시간 : ${dur.max.toFixed(2)}ms`);
    console.log(`  총 요청 수    : ${reqs.count}건`);
    console.log(`  RPS           : ${reqs.rate.toFixed(2)} req/s`);
    console.log('=============================================\n');

    return {
        'k6/polling_vu100_result.json': JSON.stringify(data, null, 2),
        stdout: textSummary(data, { indent: ' ', enableColors: true }),
    };
}
