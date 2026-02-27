import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate, Counter } from 'k6/metrics';

const myChatRoomsDuration = new Trend('my_chat_rooms_duration', true);
const errorRate = new Rate('error_rate');
const requestCount = new Counter('request_count');

export const options = {
    scenarios: {
        my_chat_rooms_steady: {
            executor: 'constant-vus',
            vus: 10,
            duration: '30s',
        },
    },
    thresholds: {
        'my_chat_rooms_duration': ['p(95)<3000', 'p(99)<5000'],
        'error_rate': ['rate<0.01'],
        'http_req_duration': ['p(95)<3000'],
    },
};

const BASE_URL = 'http://localhost:8080';
const TOKEN = __ENV.TOKEN || 'YOUR_JWT_TOKEN_HERE';

export default function () {
    const params = {
        headers: {
            'Authorization': `Bearer ${TOKEN}`,
            'Content-Type': 'application/json',
        },
    };

    const res = http.get(
        `${BASE_URL}/api/v1/users/me/chat-rooms?size=20`,
        params
    );

    myChatRoomsDuration.add(res.timings.duration);
    requestCount.add(1);

    const success = check(res, {
        'status is 200': (r) => r.status === 200,
        'response has chatRooms': (r) => {
            try {
                const body = JSON.parse(r.body);
                return body.data && body.data.chatRooms !== undefined;
            } catch (e) {
                return false;
            }
        },
        'response time < 3000ms': (r) => r.timings.duration < 3000,
    });

    errorRate.add(!success);
    sleep(1);
}

export function handleSummary(data) {
    const m = data.metrics.my_chat_rooms_duration.values;

    function fmt(v) {
        if (v === undefined || v === null) return 'N/A';
        return v.toFixed(2) + 'ms';
    }

    console.log('\n======= getMyChatRooms 4N+1 베이스라인 측정 결과 =======');
    console.log(`총 요청 수: ${data.metrics.request_count.values.count}`);
    console.log(`에러율: ${(data.metrics.error_rate.values.rate * 100).toFixed(2)}%`);
    console.log(`응답시간 평균: ${fmt(m.avg)}`);
    console.log(`응답시간 p90: ${fmt(m['p(90)'])}`);
    console.log(`응답시간 p95: ${fmt(m['p(95)'])}`);
    console.log(`응답시간 max: ${fmt(m.max)}`);
    console.log('=======================================================\n');

    return {
        'k6/my_chat_rooms_result.json': JSON.stringify(data, null, 2),
    };
}