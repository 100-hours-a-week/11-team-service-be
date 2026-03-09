/**
 * WebSocket + STOMP 개선 후 측정 - VU 100명
 *
 * 목적:
 *   폴링 VU 100명 baseline 대비 WebSocket 구조에서의 서버 부하 비교.
 *   핵심 지표: Prometheus acquire_count, jvm_threads_live 변화량
 *
 *   폴링 VU 100명 baseline:
 *   - acquire_count 2초당 +100 (100명 × 2초 주기 GET)
 *   - jvm_threads_live: 123
 *
 *   WebSocket 전환 후 예상:
 *   - acquire_count 2초당 증가 없음 (새 메시지 없으면 DB 접근 없음)
 *   - jvm_threads_live: 대폭 감소 예상
 *
 * 조건:
 *   - VU: 100명
 *   - Duration: 30s
 *   - WebSocket 연결 유지 (SockJS over WebSocket)
 *   - 메시지 전송: REST POST
 *
 * 실행:
 *   k6 run k6/websocket_vu100.js
 *
 * 결과 파일:
 *   k6/websocket_vu100_result.json
 */
import ws from 'k6/ws';
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

function getSockJsWsUrl() {
    const serverId = Math.floor(Math.random() * 1000).toString().padStart(3, '0');
    const sessionId = [...Array(8)].map(() => Math.random().toString(36)[2]).join('');
    return `ws://localhost:8080/ws/${serverId}/${sessionId}/websocket`;
}

function stompFrame(command, headers = {}, body = '') {
    let frame = command + '\n';
    for (const [k, v] of Object.entries(headers)) {
        frame += `${k}:${v}\n`;
    }
    frame += '\n' + body + '\x00';
    return frame;
}

function sockjsSend(socket, msg) {
    socket.send(JSON.stringify([msg]));
}

export function setup() {
    const tokenRes = http.get('http://localhost:8080/api/test/token?userId=1&role=USER');
    const body = JSON.parse(tokenRes.body);
    const token = body.data.accessToken;
    console.log(`[setup] 토큰 발급 완료: ${token.substring(0, 20)}...`);
    return { token };
}

export default function ({ token }) {
    const wsUrl = getSockJsWsUrl();
    let subscribed = false;

    const response = ws.connect(`${wsUrl}?token=${token}`, {}, function (socket) {

        socket.on('message', (data) => {
            if (data === 'o') {
                sockjsSend(socket, stompFrame('CONNECT', {
                    'accept-version': '1.2',
                    'heart-beat': '0,0',
                }));
                return;
            }

            if (data === 'h') return;

            if (data.startsWith('a')) {
                try {
                    const messages = JSON.parse(data.slice(1));
                    for (const msg of messages) {
                        if (msg.startsWith('CONNECTED')) {
                            sockjsSend(socket, stompFrame('SUBSCRIBE', {
                                id: 'sub-0',
                                destination: '/topic/chat-rooms/1',
                            }));
                            subscribed = true;
                        }
                    }
                } catch (e) {}
            }
        });

        socket.on('error', (e) => {
            console.log(`[ws error] ${e}`);
        });

        socket.setTimeout(() => {
            if (subscribed) {
                const headers = { Authorization: `Bearer ${token}` };
                const res = http.post(
                    'http://localhost:8080/api/v1/chat-rooms/1/messages',
                    { messageType: 'TEXT', content: '[k6] VU 메시지' },
                    { headers }
                );
                check(res, { 'message sent 200': (r) => r.status === 200 || r.status === 201 });
            }
        }, 500);

        socket.setTimeout(() => {
            socket.close();
        }, 28000);
    });

    check(response, { 'ws connected': (r) => r && r.status === 101 });

    sleep(2);
}

export function handleSummary(data) {
    const dur = data.metrics.http_req_duration ? data.metrics.http_req_duration.values : {};
    const reqs = data.metrics.http_reqs ? data.metrics.http_reqs.values : {};

    console.log('\n========== WebSocket VU 100명 결과 요약 ==========');
    if (dur.avg !== undefined) {
        console.log(`  avg 응답 시간 : ${dur.avg.toFixed(2)}ms`);
        console.log(`  p90 응답 시간 : ${dur['p(90)'].toFixed(2)}ms`);
        console.log(`  p95 응답 시간 : ${dur['p(95)'].toFixed(2)}ms`);
        console.log(`  max 응답 시간 : ${dur.max.toFixed(2)}ms`);
    }
    if (reqs.count !== undefined) {
        console.log(`  총 요청 수    : ${reqs.count}건`);
        console.log(`  RPS           : ${reqs.rate.toFixed(2)} req/s`);
    }
    console.log('==================================================\n');

    return {
        'k6/websocket_vu100_result.json': JSON.stringify(data, null, 2),
        stdout: textSummary(data, { indent: ' ', enableColors: true }),
    };
}
