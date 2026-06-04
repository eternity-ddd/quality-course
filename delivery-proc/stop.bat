@echo off
REM 백엔드(8081) + 프론트엔드(5174) 중단. (Windows)
setlocal enabledelayedexpansion
set found=0

for %%P in (8081 5174) do (
  for /f "tokens=5" %%I in ('netstat -ano ^| findstr ":%%P " ^| findstr LISTENING') do (
    echo [*] 포트 %%P 종료: PID %%I
    taskkill /F /PID %%I >nul 2>nul
    set found=1
  )
)

if "!found!"=="0" (
  echo 실행 중인 서버가 없습니다.
) else (
  echo [OK] 중단 완료.
)
endlocal
