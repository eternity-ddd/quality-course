#!/usr/bin/env bash
# 백엔드(8081) + 프론트엔드(5174)를 한 번에 띄운다. (macOS / Linux)
set -uo pipefail
cd "$(dirname "$0")"

mkdir -p logs

command -v java >/dev/null 2>&1 || { echo "❌ java(JDK 17)가 필요합니다. README.md 참고."; exit 1; }
command -v node >/dev/null 2>&1 || { echo "❌ node(20+)가 필요합니다. README.md 참고."; exit 1; }

if lsof -ti tcp:8081 >/dev/null 2>&1 || lsof -ti tcp:5174 >/dev/null 2>&1; then
  echo "⚠️  8081/5174 포트가 이미 사용 중입니다. 먼저 ./stop.sh 를 실행하세요."
  exit 1
fi

echo "▶ 백엔드 기동 (로그: logs/backend.log)..."
nohup ./gradlew :backend:bootRun > logs/backend.log 2>&1 &

echo "▶ 프론트 의존성 확인..."
( cd frontend/api && [ -d node_modules ] || npm install )

echo "▶ 프론트 기동 (로그: logs/frontend.log)..."
nohup sh -c 'cd frontend/api && npm run dev' > logs/frontend.log 2>&1 &

echo "▶ 백엔드 준비 대기..."
if curl -s --retry 60 --retry-delay 2 --retry-connrefused --retry-all-errors \
     "http://localhost:8081/api/shops?lat=37.4979&lng=127.0276&size=1" -o /dev/null; then
  echo "  ✅ 백엔드 OK (http://localhost:8081)"
else
  echo "  ⚠️ 백엔드 준비 시간 초과 — logs/backend.log 확인"
fi

echo "▶ 프론트 준비 대기..."
if curl -s --retry 30 --retry-delay 1 --retry-connrefused "http://localhost:5174" -o /dev/null; then
  echo "  ✅ 프론트 OK"
else
  echo "  ⚠️ 프론트 준비 시간 초과 — logs/frontend.log 확인"
fi

echo
echo "✅ 실행 완료 → 브라우저에서  http://localhost:5174  접속"
echo "   중단하려면  ./stop.sh"
