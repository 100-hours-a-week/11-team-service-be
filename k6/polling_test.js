import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate, Counter } from 'k6/metrics';

const messageListDuration = new Trend('message_list_duration', true);
const errorRate = new Rate('error_rate');
const requestCount = new Counter('request_count');

export const options = {
    scenarios: {
        polling_steady: {
            executor: 'constant-vus',
            vus: 10,
            duration: '30s',
        },
    },
    thresholds: {
        'message_list_duration': ['p(95)<500', 'p(99)<1000'],
        'error_rate': ['rate<0.01'],
        'http_req_duration': ['p(95)<500'],
    },
};

const BASE_URL = 'http://localhost:8080';
const CHAT_ROOM_ID = 1;
const POLLING_INTERVAL = 2;
const TOKEN = __ENV.TOKEN || 'YOUR_JWT_TOKEN_HERE';

export default function () {
    const params = {
        headers: {
            'Authorization': `Bearer ${TOKEN}`,
            'Content-Type': 'application/json',
        },
    };

    const res = http.get(
        `${BASE_URL}/api/v1/chat-rooms/${CHAT_ROOM_ID}/messages`,
        params
    );

    messageListDuration.add(res.timings.duration);
    requestCount.add(1);

    const success = check(res, {
        'status is 200': (r) => r.status === 200,
        'response has messages': (r) => {
            try {
                const body = JSON.parse(r.body);
                return body.data && body.data.messages !== undefined;
            } catch (e) {
                return false;
            }
        },
        'response time < 500ms': (r) => r.timings.duration < 500,
    });

    errorRate.add(!success);
    sleep(POLLING_INTERVAL);
}

export function handleSummary(data) {
    const m = data.metrics.message_list_duration.values;
    const p99 = m['p(99)'] !== undefined ? m['p(99)'] : m['p(99.0)'];

    console.log('\n======= 폴링 성능 베이스라인 측정 결과 =======');
    console.log(`총 요청 수: ${data.metrics.request_count.values.count}`);
    console.log(`에러율: ${(data.metrics.error_rate.values.rate * 100).toFixed(2)}%`);
    console.log(`응답시간 평균: ${m.avg.toFixed(2)}ms`);
    console.log(`응답시간 p90: ${m['p(90)'].toFixed(2)}ms`);
    console.log(`응답시간 p95: ${m['p(95)'].toFixed(2)}ms`);
    console.log(`응답시간 max: ${m.max.toFixed(2)}ms`);
    console.log('==============================================\n');

    return {
        'k6/polling_result.json': JSON.stringify(data, null, 2),
    };
}
