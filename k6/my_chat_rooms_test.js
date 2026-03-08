import http from 'k6/http';
import { check, sleep } from 'k6';
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.0.1/index.js';

// 4N+1 개선 전/후 성능 측정
// 대상 API: GET /api/v1/users/me/chat-rooms?size=20
// 조회 주체: userId=1 (채팅방 21개 참여 상태)
// 필요 시드: V2 + V7
export const options = {
    vus: 10,
    duration: '30s',
    thresholds: {
        http_req_failed: ['rate<0.01'],
        http_req_duration: ['p(95)<3000'],
    },
};

const BASE_URL = 'http://localhost:8080';
const TOKEN = __ENV.TOKEN;

export default function () {
    const res = http.get(`${BASE_URL}/api/v1/users/me/chat-rooms?size=20`, {
        headers: {
            Authorization: `Bearer ${TOKEN}`,
        },
    });

    check(res, {
        'status is 200': (r) => r.status === 200,
        'has chatRooms': (r) => {
            const body = JSON.parse(r.body);
            return body.data && body.data.chatRooms !== undefined;
        },
    });

    sleep(1);
}

export function handleSummary(data) {
    return {
        'k6/my_chat_rooms_result.json': JSON.stringify(data, null, 2),
        stdout: textSummary(data, { indent: ' ', enableColors: true }),
    };
}
