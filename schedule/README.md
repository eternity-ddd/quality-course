# schedule

객체지향 **설계 원칙**을 일정(Schedule) 도메인으로 연습하는 예제 프로젝트입니다.
반복 일정(주간/월간)을 다형성으로 분리하고 JSON 변환을 책임에 맞게 나누는 리팩터링 결과를 담고 있습니다.

DB·서버·Docker가 없는 **순수 Java + 테스트** 프로젝트라, JDK만 있으면 바로 테스트를 돌릴 수 있습니다.
빌드 도구(Gradle)는 **래퍼(`gradlew`)가 포함**되어 별도 설치가 필요 없습니다.

---

## 1. 사전 준비물

| 도구 | 버전 | 비고 |
|------|------|------|
| JDK (Java) | **17** | 이것만 있으면 됩니다 |

> Gradle은 설치 불필요 — 동봉된 `./gradlew`가 최초 실행 시 Gradle을 자동으로 내려받습니다.

### JDK 17 설치

**macOS**

```bash
brew install --cask temurin@17
# 또는 SDKMAN
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk install java 17.0.13-tem
```

**Linux**

```bash
sudo apt update && sudo apt install -y openjdk-17-jdk    # Ubuntu/Debian
sudo dnf install -y java-17-openjdk-devel                # Fedora/RHEL
```

확인:

```bash
java -version    # 17.x.x 이면 OK
```

---

## 2. 테스트 실행

이 프로젝트는 실행 가능한 애플리케이션이 아니라 **테스트로 설계를 검증**하는 예제입니다.

**macOS / Linux**

```bash
./gradlew test
```

**Windows**

```bat
gradlew.bat test
```

- 최초 실행 시 Gradle 배포본을 자동 다운로드하므로 시간이 조금 걸립니다(이후엔 빠름).
- 모든 테스트가 통과하면 `BUILD SUCCESSFUL`이 출력됩니다. (현재 22개 테스트)

컴파일만:

```bash
./gradlew compileJava          # mac/linux
gradlew.bat compileJava        # windows
```

---

## 3. 구조

같은 일정(Schedule) 도메인을 두 단계로 담았습니다. **step02 → step03 순서로 설계가 어떻게 다듬어지는지** 비교하며 보세요.

### step02 — 중간 단계

`src/main/java/org/eternity/event/step02`

| 클래스 | 역할 |
|--------|------|
| `Schedule` | 일정 + 반복 규칙을 한 클래스에서 직접 처리 |
| `Scheduler` | 특정 날짜에 포함되는 일정 처리 |
| `JsonConverter` | 일정 ↔ JSON 변환 |

### step03 — 최종 설계

`src/main/java/org/eternity/event/step03`

| 클래스 | 역할 |
|--------|------|
| `Schedule` | 반복 여부 판단을 `RecurringPlan`에 위임 |
| `RecurringPlan` | 반복 규칙 추상화 (다형성의 중심) |
| `WeeklyPlan` / `MonthlyPlan` | 주간 / 월간 반복 규칙 구현 |
| `Scheduler` | 특정 날짜에 포함되는 일정을 모아 처리 |
| `JsonConverter` / `JacksonConverter` / `ScheduleJson` | 일정 ↔ JSON 변환 책임 분리 |

각 단계의 테스트는 `src/test/java/org/eternity/event/<step>` 에 대응됩니다.

> 참고: 원본 강의 자료에는 초기 버전(step01)도 있었으나, 본 배포본은 step02·step03만 남겼습니다.
