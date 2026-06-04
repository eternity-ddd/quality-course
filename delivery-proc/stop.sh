#!/usr/bin/env bash
# 백엔드(8081) + 프론트엔드(5174) 중단. (macOS / Linux)
cd "$(dirname "$0")"

stopped=0
for port in 8081 5174; do
  pids=$(lsof -ti tcp:$port 2>/dev/null || true)
  if [ -n "$pids" ]; then
    echo "▶ 포트 $port 종료 (PID: $pids)"
    echo "$pids" | xargs kill -9 2>/dev/null || true
    stopped=1
  fi
done

if [ "$stopped" -eq 0 ]; then
  echo "실행 중인 서버가 없습니다."
else
  echo "✅ 중단 완료."
fi
