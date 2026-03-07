import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate, Counter } from 'k6/metrics';

const duration = new Trend('get_chat_rooms_by_job_posting_duration', true);
const errorRate = new Rate('error_rate');
const requestCount = new Counter('request_count');

export const options = {
    scenarios: {
        get_chat_rooms_by_job_posting_steady: {
            executor: 'constant-vus',
            vus: 10,
            duration: '30s',
        },
    },
    thresholds: {
        'get_chat_rooms_by_job_posting_duration': ['p(95)<3000', 'p(99)<5000'],
        'error_rate': ['rate<0.01'],
        'http_req_duration': ['p(95)<3000'],
    },
};

const BASE_URL = 'http://localhost:8080';
const JOB_MASTER_ID = 1;
const TOKEN = __ENV.TOKEN || 'YOUR_JWT_TOKEN_HERE';

export default function () {
    const params = {
        headers: {
            'Authorization': `Bearer ${TOKEN}`,
            'Content-Type': 'application/json',
        },
    };

    const res = http.get(
        `${BASE_URL}/api/v1/job-postings/${JOB_MASTER_ID}/chat-rooms?size=10`,
        params
    );

    duration.add(res.timings.duration);
    requestCount.add(1);

    const success = check(res, {
        'status is 200': (r) => r.status === 200,
        'response time < 3000ms': (r) => r.timings.duration < 3000,
    });

    errorRate.add(!success);
    sleep(1);
}

export function handleSummary(data) {
    const m = data.metrics.get_chat_rooms_by_job_posting_duration.values;

    console.log('\n======= getChatRoomsByJobPosting 8N+1 개선 후 측정 결과 =======');
    console.log(`총 요청 수: ${data.metrics.request_count.values.count}`);
    console.log(`에러율: ${(data.metrics.error_rate.values.rate * 100).toFixed(2)}%`);
    console.log(`응답시간 평균: ${m.avg.toFixed(2)}ms`);
    console.log(`응답시간 p90: ${m['p(90)'].toFixed(2)}ms`);
    console.log(`응답시간 p95: ${m['p(95)'].toFixed(2)}ms`);
    console.log(`응답시간 max: ${m.max.toFixed(2)}ms`);
    console.log('=================================================================\n');

    return {
        'k6/get_chat_rooms_by_job_posting_result.json': JSON.stringify(data, null, 2),
    };
}
