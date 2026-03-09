#!/bin/bash
# =====================================================================
# Prometheus 실시간 모니터링 스크립트
#
# 목적: k6 부하 테스트 중 서버 관점 메트릭을 실시간으로 확인
#       스크린샷으로 개선 전 baseline 기록
#
# 측정 항목:
#   - hikaricp_connections_active  : 활성 DB 커넥션 수
#   - hikaricp_connections_pending : 커넥션 풀 대기 수
#   - hikaricp_acquire_count (누적): 폴링 요청마다 DB까지 전달된 횟수 (핵심)
#   - jvm_threads_live             : JVM 활성 스레드 수
#   - http_request_count /messages : 실제 처리된 요청 수
#
# 스크린샷 타이밍:
#   ① k6 실행 전          → baseline 값 기록
#   ② VU 10 부하 중 ~10초 → VU 10 최고점 기록
#   ③ VU 100 부하 중 ~10초→ VU 100 최고점 기록
#
# 실행:
#   chmod +x k6/prometheus_monitor.sh
#   ./k6/prometheus_monitor.sh
# =====================================================================

PROMETHEUS_URL="http://localhost:9101/actuator/prometheus"

echo "======================================================"
echo "  Prometheus 실시간 모니터링 (2초 주기)"
echo "  대상: ${PROMETHEUS_URL}"
echo "======================================================"
echo ""
echo "  📌 스크린샷 타이밍 안내"
echo "  ① 지금 (k6 실행 전)    → baseline"
echo "  ② VU 10 k6 실행 ~10초  → VU 10 부하 중 최고점"
echo "  ③ VU 100 k6 실행 ~10초 → VU 100 부하 중 최고점"
echo ""
echo "  Ctrl+C 로 종료"
echo "======================================================"
echo ""

while true; do
    RAW=$(curl -s "$PROMETHEUS_URL")

    if [ -z "$RAW" ]; then
        echo "$(date '+%H:%M:%S') ❌ Prometheus 응답 없음 — 서버가 기동 중인지 확인하세요"
        sleep 2
        continue
    fi

    # hikaricp_connections_active (pool 이름 무관하게 합산)
    HIKARI_ACTIVE=$(echo "$RAW" | grep '^hikaricp_connections_active{' | awk '{sum += $2} END {printf "%.0f", sum}')

    # hikaricp_connections_pending
    HIKARI_PENDING=$(echo "$RAW" | grep '^hikaricp_connections_pending{' | awk '{sum += $2} END {printf "%.0f", sum}')

    # hikaricp_connections_acquire_seconds_count (누적 횟수) — 핵심 지표
    # 의도: 폴링은 매 요청마다 커넥션 획득 → 2초당 VU 수만큼 증가
    #       WebSocket 전환 후에는 새 메시지 없으면 증가 없어야 함
    HIKARI_ACQUIRE=$(echo "$RAW" | grep 'hikaricp_connections_acquire_seconds_count{' | awk '{sum += $2} END {printf "%.0f", sum}')

    # jvm_threads_live_threads
    JVM_THREADS=$(echo "$RAW" | grep '^jvm_threads_live_threads{' | awk '{print $2}')

    # http_server_requests_seconds_count — /messages 엔드포인트 필터링
    HTTP_COUNT=$(echo "$RAW" | grep 'http_server_requests_seconds_count' | grep 'messages' | awk '{sum += $2} END {printf "%.0f", sum}')

    echo "──────────────────────────────────────────────────────"
    printf "  %s\n" "$(date '+%Y-%m-%d %H:%M:%S')"
    printf "  hikaricp_connections_active        : %s\n" "${HIKARI_ACTIVE:-N/A}"
    printf "  hikaricp_connections_pending       : %s\n" "${HIKARI_PENDING:-N/A}"
    printf "  hikaricp_acquire_count (누적)       : %s\n" "${HIKARI_ACQUIRE:-N/A}"
    printf "  jvm_threads_live                   : %s\n" "${JVM_THREADS:-N/A}"
    printf "  http_request_count /messages (누적): %s\n" "${HTTP_COUNT:-N/A}"

    sleep 2
done
