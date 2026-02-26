/**
 * [성능 테스트] 채팅 메시지 폴링 방식 부하 테스트
 *
 * 목적: 폴링 방식에서 동시 사용자 증가 시 응답 시간/TPS 측정
 *      → WebSocket 전환 후 동일 시나리오로 비교하기 위한 베이스라인 측정
 *
 * 시나리오: 팀원(#148 이슈)과 동일한 4단계 부하 구성
 *   - 초기 진입 30초: 0 → 200 VU
 *   - 부하 가중 1분:  200 → 500 VU
 *   - 스트레스 1분:  500 → 1000 VU 유지
 *   - 종료 30초:    1000 → 0 VU
 *
 * 임계값:
 *   - p(95) < 1000ms
 *   - 에러율 < 1%
 *
 * 실행 방법:
 *   k6 run docs/k6/load-test-chat-polling.js
 *
 * 결과 저장:
 *   k6 run --out json=docs/k6/result-polling-before.json docs/k6/load-test-chat-polling.js
 */

import http from 'k6/http';
import { check, sleep } from 'k6';

// ── 설정 ────────────────────────────────────────────────
const BASE_URL = 'http://localhost:8080';
const CHAT_ROOM_ID = 1; // 시드 데이터 chat_room_id

// 시드 데이터 유저 토큰 (테스트 전 /dev/token?userId=1 로 발급받아서 여기에 붙여넣기)
// 토큰 만료(30분) 시 재발급 필요
const ACCESS_TOKEN = __ENV.TOKEN || 'YOUR_DEV_TOKEN_HERE';

export const options = {
    stages: [
        { duration: '30s', target: 200  }, // 초기 진입
        { duration: '1m',  target: 500  }, // 부하 가중
        { duration: '1m',  target: 1000 }, // 스트레스
        { duration: '30s', target: 0    }, // 종료
    ],
    thresholds: {
        http_req_duration: ['p(95)<1000'], // p95 1초 이내
        http_req_failed:   ['rate<0.01'], // 에러율 1% 미만
    },
};

// ── 공통 헤더 ────────────────────────────────────────────
const headers = {
    Authorization: `Bearer ${ACCESS_TOKEN}`,
    'Content-Type': 'application/json',
};

// ── 메인 테스트 시나리오 ──────────────────────────────────
export default function () {
    // [폴링 시나리오] 2초마다 최신 메시지 조회 (cursor 없이 첫 페이지)
    const res = http.get(
        `${BASE_URL}/api/v1/chat-rooms/${CHAT_ROOM_ID}/messages`,
        { headers }
    );

    check(res, {
        '채팅 메시지 조회 200': (r) => r.status === 200,
        '응답 body 존재': (r) => r.body && r.body.length > 0,
    });

    sleep(2); // 클라이언트 폴링 주기 2초
}
