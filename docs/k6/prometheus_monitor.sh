#!/bin/bash
# ============================================================
# Prometheus 지표 모니터링 스크립트 (동기 블로킹 테스트용)
#
# 목적: k6 부하 테스트 실행 중 서버 스레드 점유 현상을 실시간으로 확인
# 실행: chmod +x docs/k6/prometheus_monitor.sh && ./docs/k6/prometheus_monitor.sh
#
# 핵심 지표:
#   jvm_threads_states{state="timed-waiting"}
#     → Thread.sleep() 호출 시 스레드 상태가 timed-waiting으로 전환됨
#     → AI 동기 블로킹 요청이 동시에 몰릴수록 이 값이 VU 수만큼 증가
#     → tomcat_threads_busy 가 없는 환경에서 블로킹 현상의 핵심 지표
#
#   jvm_threads_live       → 전체 라이브 스레드 수 (전체 추세 파악)
#   jvm_threads_peak       → 피크 스레드 수 (테스트 후 최대값 확인)
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

  # 핵심 지표: timed-waiting 상태 스레드 수
  # Thread.sleep() 호출 시 해당 스레드가 timed-waiting으로 전환됨
  # → 동시 요청 VU 수만큼 이 값이 올라가면 블로킹 현상 재현 성공
  TIMED_WAITING=$(echo "$RAW" | grep 'jvm_threads_states_threads' | grep 'timed-waiting' | awk '{print $2}')

  # 블로킹 상태 스레드 수 (DB 락 경합 등)
  BLOCKED=$(echo "$RAW" | grep 'jvm_threads_states_threads' | grep 'state="blocked"' | awk '{print $2}')

  # 전체 라이브 스레드
  LIVE=$(echo "$RAW" | grep '^jvm_threads_live_threads' | awk '{print $2}')

  # 피크 스레드 (누적 최대값)
  PEAK=$(echo "$RAW" | grep '^jvm_threads_peak_threads' | awk '{print $2}')

  echo "[$TIMESTAMP]"
  echo "  🔴 timed-waiting threads  : ${TIMED_WAITING:-N/A}  ← AI sleep 점유 스레드 (핵심)"
  echo "  🟠 blocked threads        : ${BLOCKED:-N/A}        ← DB 락 경합 스레드"
  echo "  🟡 jvm_threads_live       : ${LIVE:-N/A}"
  echo "  🔵 jvm_threads_peak       : ${PEAK:-N/A}  ← 테스트 중 최대값"
  echo "------------------------------------------------------------"

  sleep $INTERVAL
done
