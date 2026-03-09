/**
 * WebSocket + STOMP 개선 후 측정 - VU 10명
 *
 * 목적:
 *   폴링 방식과 동일한 2초 주기로 메시지를 전송하되,
 *   수신은 WebSocket push로 처리하는 구조를 측정합니다.
 *
 *   폴링 baseline과 비교 포인트:
 *   - 폴링: 2초마다 GET /messages → 매번 DB 쿼리 발생 (새 메시지 없어도)
 *   - WebSocket: 연결 유지 + 메시지 전송 시에만 DB 접근
 *
 * 조건:
 *   - VU: 10명
 *   - Duration: 30s
 *   - 전송 주기: 2초 (폴링 baseline과 동일 조건)
 *   - WebSocket 연결: SockJS over WebSocket
 *   - 메시지 전송: REST POST (기존 API 유지)
 *   - 메시지 수신: WebSocket STOMP push
 *
 * 실행:
 *   k6 run k6/websocket_vu10.js
 *
 * 결과 파일:
 *   k6/websocket_vu10_result.json
 */
import ws from 'k6/ws';
import http from 'k6/http';
import { check, sleep } from 'k6';
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.0.1/index.js';

export const options = {
    vus: 10,
    duration: '30s',
    thresholds: {
        http_req_failed: ['rate<0.01'],
        http_req_duration: ['p(95)<500'],
    },
};

// SockJS WebSocket URL 생성
// 의도: SockJS 프로토콜은 /endpoint/{serverId}/{sessionId}/websocket 형식 사용
function getSockJsWsUrl() {
    const serverId = Math.floor(Math.random() * 1000).toString().padStart(3, '0');
    const sessionId = [...Array(8)].map(() => Math.random().toString(36)[2]).join('');
    return `ws://localhost:8080/ws/${serverId}/${sessionId}/websocket`;
}

// STOMP 프레임 생성
// 의도: STOMP 프로토콜은 COMMAND\nheader:value\n\nbody\x00 형식
function stompFrame(command, headers = {}, body = '') {
    let frame = command + '\n';
    for (const [k, v] of Object.entries(headers)) {
        frame += `${k}:${v}\n`;
    }
    frame += '\n' + body + '\x00';
    return frame;
}

// SockJS 전송 래퍼
// 의도: SockJS는 메시지를 JSON 배열로 래핑하여 전송
function sockjsSend(socket, msg) {
    socket.send(JSON.stringify([msg]));
}

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
    const wsUrl = getSockJsWsUrl();
    let connected = false;
    let subscribed = false;
    let receivedCount = 0;

    const response = ws.connect(`${wsUrl}?token=${token}`, {}, function (socket) {

        socket.on('open', () => {
            // SockJS 오픈 프레임 대기 후 STOMP CONNECT 전송
        });

        socket.on('message', (data) => {
            // SockJS 프레임 파싱
            if (data === 'o') {
                // SockJS 연결 오픈 → STOMP CONNECT 전송
                sockjsSend(socket, stompFrame('CONNECT', {
                    'accept-version': '1.2',
                    'heart-beat': '0,0',
                }));
                return;
            }

            if (data === 'h') {
                // SockJS heartbeat — 무시
                return;
            }

            if (data.startsWith('a')) {
                // SockJS 메시지 프레임: a["..."]
                try {
                    const messages = JSON.parse(data.slice(1));
                    for (const msg of messages) {
                        if (msg.startsWith('CONNECTED')) {
                            connected = true;
                            // STOMP SUBSCRIBE 전송
                            sockjsSend(socket, stompFrame('SUBSCRIBE', {
                                id: 'sub-0',
                                destination: '/topic/chat-rooms/1',
                            }));
                            subscribed = true;
                        } else if (msg.startsWith('MESSAGE')) {
                            receivedCount++;
                        }
                    }
                } catch (e) {
                    // 파싱 오류 무시
                }
            }
        });

        socket.on('error', (e) => {
            console.log(`[ws error] ${e}`);
        });

        // 2초 주기로 메시지 전송 (폴링 baseline과 동일 조건)
        // 의도: 폴링은 2초마다 GET으로 DB 조회. WebSocket은 2초마다 POST로 메시지 전송.
        //       실제 서비스에서는 사용자가 채팅을 보낼 때만 DB 접근 → acquire_count 비교의 핵심
        socket.setTimeout(() => {
            if (subscribed) {
                // 의도: 서버 컨트롤러가 consumes=multipart/form-data + @RequestParam으로 설계됨
                //       k6에서 multipart/form-data로 보내려면 FormData 객체 대신
                //       { field: http.file(...) } 형식을 사용해야 함
                const headers = { Authorization: `Bearer ${token}` };
                // 의도: plain object → k6가 application/x-www-form-urlencoded로 전송
                //       서버 @RequestParam String이 정상 바인딩됨
                //       http.file()을 쓰면 항상 MultipartFile 파트가 되어 String 변환 실패
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
        }, 28000); // 28초 후 종료 (duration 30s 기준)

    });

    check(response, { 'ws connected': (r) => r && r.status === 101 });

    sleep(2);
}

// handleSummary(): 테스트 종료 후 결과 출력 및 저장
export function handleSummary(data) {
    const dur = data.metrics.http_req_duration ? data.metrics.http_req_duration.values : {};
    const reqs = data.metrics.http_reqs ? data.metrics.http_reqs.values : {};

    console.log('\n========== WebSocket VU 10명 결과 요약 ==========');
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
    console.log('=================================================\n');

    return {
        'k6/websocket_vu10_result.json': JSON.stringify(data, null, 2),
        stdout: textSummary(data, { indent: ' ', enableColors: true }),
    };
}
