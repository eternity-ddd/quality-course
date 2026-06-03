# quality-course

코드 품질 · 객체지향 설계 강의 교보재 모음입니다.
각 프로젝트는 **독립적으로** 빌드·실행됩니다 — 한꺼번에 빌드하는 멀티 모듈이 아니라,
**필요한 폴더로 들어가 해당 프로젝트의 README를 따르는** 구조입니다.

---

## 프로젝트

| 폴더 | 내용 | 스택 | 상세 |
|------|------|------|------|
| [`delivery-oo`](delivery-oo/README.md) | 음식 배달 도메인 — **객체지향(OO/DDD)** 구현 | Spring Boot + React | [README](delivery-oo/README.md) |
| [`delivery-proc`](delivery-proc/README.md) | 음식 배달 도메인 — **절차적** 구현 | Spring Boot + React | [README](delivery-proc/README.md) |
| [`schedule`](schedule/README.md) | 일정 도메인 — **객체지향 설계 원칙** 예제 | 순수 Java | [README](schedule/README.md) |

> `delivery-oo` ↔ `delivery-proc` 는 **같은 도메인을 정반대 스타일로 구현한 비교용 한 쌍**입니다.
> 두 개를 번갈아(또는 나란히) 열어 설계 차이를 비교해 보세요.

---

## 사전 준비물 (공통)

| 도구 | 버전 | 필요한 곳 |
|------|------|-----------|
| JDK (Java) | **17** | 전 프로젝트 |
| Node.js | **20+** | `delivery-*` 프론트엔드 |

- 빌드 도구(Gradle)는 각 프로젝트에 **래퍼(`gradlew`)가 포함**되어 별도 설치가 필요 없습니다.
- `delivery-*` 는 **인메모리 H2**라 DB·Docker 설치가 필요 없습니다.
- 설치 방법(OS별)은 각 프로젝트 README의 "사전 준비물" 절을 참고하세요.

---

## 빠른 시작

### delivery-oo / delivery-proc (백엔드 + 프론트엔드)

```bash
cd delivery-oo          # 또는 delivery-proc
./start.sh              # macOS/Linux: 백엔드(8080) + 프론트(5173) 한 번에
./stop.sh               # 중단
# Windows: start.bat / stop.bat
```

브라우저에서 **http://localhost:5173** 접속. 자세한 건 해당 폴더 README 참고.

### schedule (테스트로 설계 검증)

```bash
cd schedule
./gradlew test          # macOS/Linux  (Windows: gradlew.bat test)
```

---

## IDE에서 열기

각 프로젝트가 독립 빌드라, **하위 폴더를 각각 별도 프로젝트로 import** 하는 것을 권장합니다.
(IntelliJ 기준: `delivery-oo`, `delivery-proc`, `schedule` 폴더를 각각 "Open"
— 루트 `quality-course`를 열면 셋이 한 빌드로 묶이지 않으니 주의)

---

## 주의

- **포트 충돌**: `delivery-oo` 와 `delivery-proc` 는 둘 다 백엔드 8080 / 프론트 5173 을 씁니다.
  한 번에 **하나의 delivery 프로젝트만** 띄우세요.
- 이 저장소는 멀티 모듈이 **아닙니다**. 루트에는 `settings.gradle` 이 없으며, 빌드/실행은 각 폴더 안에서 합니다.
