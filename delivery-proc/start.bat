@echo off
REM 백엔드(8081) + 프론트엔드(5174)를 각각 새 창으로 띄운다. (Windows)
cd /d "%~dp0"

where java >nul 2>nul || (echo [X] java(JDK 17)가 필요합니다. README.md 참고. & pause & exit /b 1)
where node >nul 2>nul || (echo [X] node(20+)가 필요합니다. README.md 참고. & pause & exit /b 1)

echo [*] 프론트 의존성 확인...
if not exist "frontend\api\node_modules" (
  pushd frontend\api && call npm install && popd
)

echo [*] 백엔드 창을 엽니다 (port 8081)...
start "delivery backend" cmd /k "gradlew.bat :backend:bootRun"

echo [*] 프론트 창을 엽니다 (port 5174)...
start "delivery frontend" cmd /k "cd frontend\api ^&^& npm run dev"

echo.
echo [OK] 두 창이 떴습니다. 기동까지 30초 정도 기다린 뒤
echo      브라우저에서  http://localhost:5174  접속하세요.
echo      중단하려면 각 창을 닫거나  stop.bat  실행.
