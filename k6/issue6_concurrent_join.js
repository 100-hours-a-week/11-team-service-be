import http from 'k6/http';
import { check } from 'k6';

// 시나리오 의도:
//   userId=4, userId=5가 chat_room_id=2(정원 2명, 현재 1명)에
//   동시에 입장 요청 → 둘 다 통과하면 정원 초과 저장 증명
//
// 기대(정상): 한 명만 200, 한 명은 409
// 실제(문제): 둘 다 200 → DB에 chat_room_members 2건 추가 → 총 3명

const BASE_URL = 'http://localhost:8080';
const CHAT_ROOM_ID = 2;

const TOKENS = {
    4: __ENV.TOKEN_USER4,
    5: __ENV.TOKEN_USER5,
};

export const options = {
    scenarios: {
        concurrent_join: {
            executor: 'per-vu-iterations',
            vus: 2,
            iterations: 1,
            maxDuration: '10s',
        },
    },
};

export default function () {
    const userId = __VU + 3;
    const token = TOKENS[userId];

    const res = http.post(
        `${BASE_URL}/api/v1/chat-rooms/${CHAT_ROOM_ID}/members`,
        null,
        {
            headers: {
                Authorization: `Bearer ${token}`,
                'Content-Type': 'application/json',
            },
        }
    );

    console.log(`userId=${userId} → status=${res.status} body=${res.body}`);

    check(res, {
        'status is 200 or 409': (r) => r.status === 200 || r.status === 409,
    });
}