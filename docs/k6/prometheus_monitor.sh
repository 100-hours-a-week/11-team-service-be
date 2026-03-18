#!/bin/bash
# ============================================================
# Prometheus 지표 모니터링 스크립트
# 실행: chmod +x docs/k6/prometheus_monitor.sh && ./docs/k6/prometheus_monitor.sh
# ============================================================

ACTUATOR_URL="http://localhost:9101/actuator/prometheus"
INTERVAL=2

echo "============================================================"
echo "  Prometheus 지표 모니터링 시작 (${INTERVAL}초 간격)"
echo "  종료: Ctrl+C"
echo "============================================================"
echo ""

while true; do
  TIMESTAMP=$(date '+%H:%M:%S')
  RAW=$(curl -s "$ACTUATOR_URL")

  if [ -z "$RAW" ]; then
    echo "[$TIMESTAMP] ❌ 서버 응답 없음 — 서버가 실행 중인지 확인하세요"
    sleep $INTERVAL
    continue
  fi

  TIMED_WAITING=$(echo "$RAW" | grep 'jvm_threads_states_threads' | grep 'timed-waiting' | awk '{print $2}')
  BLOCKED=$(echo "$RAW" | grep 'jvm_threads_states_threads' | grep 'state="blocked"' | awk '{print $2}')
  LIVE=$(echo "$RAW" | grep '^jvm_threads_live_threads' | awk '{print $2}')
  PEAK=$(echo "$RAW" | grep '^jvm_threads_peak_threads' | awk '{print $2}')

  echo "[$TIMESTAMP]"
  echo "  🔴 timed-waiting threads  : ${TIMED_WAITING:-N/A}  ← AI sleep 점유 스레드 (핵심)"
  echo "  🟠 blocked threads        : ${BLOCKED:-N/A}"
  echo "  🟡 jvm_threads_live       : ${LIVE:-N/A}"
  echo "  🔵 jvm_threads_peak       : ${PEAK:-N/A}"
  echo "------------------------------------------------------------"

  sleep $INTERVAL
done
