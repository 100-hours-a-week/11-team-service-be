import http from 'k6/http';
import { check, sleep } from 'k6';
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.0.1/index.js';

// =====================================================================
// [로컬 전용] getChatRoomMembers N+1 성능 테스트
// 대상 API: GET /api/v1/chat-rooms/1/members
// 조건: chat_room_id=1, 멤버 5명 (HOST 1 + MEMBER 4) — 최대 정원, 최악의 N+1 케이스
// 실행: TOKEN=$(curl -s "http://localhost:8080/dev/token?userId=1&role=USER" \
//             | python3 -c "import sys,json; print(json.load(sys.stdin)['accessToken'])")
//       TOKEN=$TOKEN k6 run k6/get_chat_room_members_test.js
// =====================================================================

export const options = {
    vus: 10,
    duration: '30s',
    thresholds: {
        // 개선 전 avg 29.15ms 기준으로 넉넉하게 설정
        http_req_duration: ['p(95)<3000'],
        http_req_failed: ['rate<0.01'],
    },
};

const BASE_URL = 'http://localhost:8080';
const CHAT_ROOM_ID = 1;

export function setup() {
    const tokenRes = http.get(`${BASE_URL}/dev/token?userId=1&role=USER`);
    const token = JSON.parse(tokenRes.body).accessToken;
    return { token };
}

export default function ({ token }) {
    const res = http.get(
        `${BASE_URL}/api/v1/chat-rooms/${CHAT_ROOM_ID}/members`,
        {
            headers: {
                Authorization: `Bearer ${token}`,
                'Content-Type': 'application/json',
            },
        }
    );

    check(res, {
        'status is 200': (r) => r.status === 200,
        'has members': (r) => {
            try {
                const body = JSON.parse(r.body);
                return Array.isArray(body.data?.members) && body.data.members.length > 0;
            } catch {
                return false;
            }
        },
    });

    sleep(1);
}

export function handleSummary(data) {
    const dur = data.metrics.http_req_duration.values;
    const failed = data.metrics.http_req_failed.values;
    const iters = data.metrics.iterations.values;

    const totalReqs = iters.count;
    const errorRate = (failed.rate * 100).toFixed(2) + '%';
    const avg  = dur['avg'].toFixed(2)   + 'ms';
    const p90  = dur['p(90)'].toFixed(2) + 'ms';
    const p95  = dur['p(95)'].toFixed(2) + 'ms';
    const max  = dur['max'].toFixed(2)   + 'ms';

    console.log('\n======= getChatRoomMembers N+1 베이스라인 측정 결과 =======');
    console.log(`총 요청 수: ${totalReqs}`);
    console.log(`에러율: ${errorRate}`);
    console.log(`응답시간 평균: ${avg}`);
    console.log(`응답시간 p90: ${p90}`);
    console.log(`응답시간 p95: ${p95}`);
    console.log(`응답시간 max: ${max}`);
    console.log('===========================================================\n');

    return {
        'k6/get_chat_room_members_result.json': JSON.stringify(data, null, 2),
        stdout: textSummary(data, { indent: ' ', enableColors: true }),
    };
}
