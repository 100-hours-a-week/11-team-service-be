#!/bin/bash
# =====================================================================
# Prometheus 실시간 모니터링 스크립트
#
# 목적: k6 부하 테스트 중 서버 관점 메트릭을 실시간으로 확인
#       스크린샷으로 개선 전 베이스라인 기록
#
# 측정 항목:
#   - hikaricp_connections_active  : 활성 DB 커넥션 수
#   - hikaricp_connections_pending : 커넥션 풀 대기 수
#   - jvm_threads_live             : JVM 활성 스레드 수
#   - http_server_requests 최대값  : 메시지 조회 API 처리 시간
#
# 스크린샷 타이밍:
#   1) k6 실행 전          → baseline 값 기록
#   2) VU 10 부하 중 ~10초 → VU 10 최고점 기록
#   3) VU 100 부하 중 ~10초→ VU 100 최고점 기록
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

    # hikaricp_connections_active : 현재 쿼리 실행 중인 커넥션 수
    # (로컬 환경에서는 쿼리가 빠르게 끝나 대부분 0 — 정상)
    HIKARI_ACTIVE=$(echo "$RAW" | grep '^hikaricp_connections_active{' | awk '{sum += $2} END {printf "%.0f", sum}')

    # hikaricp_connections_pending : 커넥션 풀 포화 시 대기 스레드 수
    HIKARI_PENDING=$(echo "$RAW" | grep '^hikaricp_connections_pending{' | awk '{sum += $2} END {printf "%.0f", sum}')

    # hikaricp_connections_acquire_seconds_count : 누적 커넥션 획득 횟수
    # 부하 전후 증가량 = 폴링 요청이 DB 레이어까지 전달된 횟수의 직접적 증거
    HIKARI_ACQUIRE_COUNT=$(echo "$RAW" | grep '^hikaricp_connections_acquire_seconds_count{' | awk '{sum += $2} END {printf "%.0f", sum}')

    # jvm_threads_live_threads : 현재 JVM 활성 스레드 수
    # 수정: 라벨({application="scuad"})이 있으므로 패턴에 { 포함
    JVM_THREADS=$(echo "$RAW" | grep '^jvm_threads_live_threads{' | awk '{print $2}')

    # http_server_requests_seconds_count : /messages API 누적 처리 요청 수
    # 수정: _max(누적 최대값 고정) 대신 _count(증가하는 누적 횟수)로 교체
    HTTP_REQ_COUNT=$(echo "$RAW" | grep '^http_server_requests_seconds_count{' | grep 'messages' | awk '{sum += $2} END {printf "%.0f", sum}')

    echo "──────────────────────────────────────────────────────"
    printf "  %s\n" "$(date '+%Y-%m-%d %H:%M:%S')"
    printf "  hikaricp_connections_active        : %s\n"  "${HIKARI_ACTIVE:-N/A}"
    printf "  hikaricp_connections_pending       : %s\n"  "${HIKARI_PENDING:-N/A}"
    printf "  hikaricp_acquire_count (누적)      : %s\n"  "${HIKARI_ACQUIRE_COUNT:-N/A}"
    printf "  jvm_threads_live                   : %s\n"  "${JVM_THREADS:-N/A}"
    printf "  http_request_count /messages (누적): %s\n"  "${HTTP_REQ_COUNT:-N/A}"

    sleep 2
done
