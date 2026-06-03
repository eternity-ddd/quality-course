# delivery-oo

음식 배달 도메인을 **객체지향(OO / DDD) 스타일**로 구현한 예제 프로젝트입니다.
(절차적 스타일로 같은 도메인을 구현한 `delivery-proc`와 비교용 한 쌍입니다.)

데이터베이스는 **인메모리 H2**를 사용하므로 **DB 설치도, Docker도 필요 없습니다.**
JDK와 Node.js만 있으면 바로 실행됩니다.

---

## 1. 사전 준비물

| 도구 | 버전 | 용도 |
|------|------|------|
| JDK (Java) | **17** | 백엔드 실행 (Gradle 포함) |
| Node.js | **20 LTS 이상** | 프론트엔드 개발 서버 |

> DB(MySQL 등) 설치 불필요 — 실행 시 인메모리 H2가 자동으로 뜨고 `schema.sql` + `data.sql`로 시드됩니다.

---

## 2. 설치

### 2-1. JDK 17

**macOS**

```bash
# Homebrew (Eclipse Temurin 17)
brew install --cask temurin@17

# 또는 SDKMAN
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk install java 17.0.13-tem
```

**Linux**

```bash
# Ubuntu / Debian
sudo apt update && sudo apt install -y openjdk-17-jdk

# Fedora / RHEL
sudo dnf install -y java-17-openjdk-devel

# 또는 SDKMAN (배포판 공통)
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk install java 17.0.13-tem
```

확인:

```bash
java -version    # 17.x.x 가 보이면 OK
```

### 2-2. Node.js 20+

> 이 프로젝트의 프론트엔드는 Vite 8을 쓰며 **Node 20.19+ (또는 22.12+)** 가 필요합니다.
> 배포판 기본 `apt install nodejs`는 버전이 낮을 수 있어 **nvm 사용을 권장**합니다.

**macOS / Linux 공통 (nvm 권장)**

```bash
curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.40.1/install.sh | bash
# 터미널 새로 열기 (또는 source ~/.nvmrc 관련 안내대로)
nvm install 20
nvm use 20
```

**macOS (Homebrew 대안)**

```bash
brew install node    # 최신 LTS 설치
```

확인:

```bash
node -v    # v20.x 이상이면 OK
npm -v
```

---

## 3. 실행

**스크립트로 한 번에 실행(권장)** 하거나, 아래 **수동 실행**을 따라도 됩니다.

### 3-0. 스크립트로 한 번에 (권장)

백엔드(8080) + 프론트엔드(5173)를 한 번에 띄우고 내립니다.

**macOS / Linux**

```bash
chmod +x start.sh stop.sh   # 최초 1회만 (실행 권한 부여)
./start.sh                  # 백엔드 + 프론트 기동 (준비되면 안내 출력)
./stop.sh                   # 둘 다 중단
```

- 로그: `logs/backend.log`, `logs/frontend.log`
- 프론트 `node_modules`가 없으면 `start.sh`가 자동으로 `npm install` 합니다.

**Windows**

```bat
start.bat   :: 백엔드/프론트가 각각 새 cmd 창으로 뜸 (창을 닫으면 종료)
stop.bat    :: 8080/5173 포트의 서버를 강제 종료
```

> 기동까지 30초 안팎. 준비되면 브라우저에서 **http://localhost:5173** 접속.

---

이하는 스크립트 없이 직접 실행하는 방법입니다. 백엔드와 프론트엔드를 **터미널 2개**로 각각 띄웁니다.

### 3-1. 백엔드 (터미널 1)

```bash
# 프로젝트 루트에서
./gradlew :backend:bootRun
```

- 최초 1회는 Gradle이 의존성을 내려받아 시간이 좀 걸립니다.
- 기동되면 **http://localhost:8080** 에서 API 응답.
- 동작 확인:

  ```bash
  curl "http://localhost:8080/api/shops?lat=37.4979&lng=127.0276&size=3"
  ```

  강남역 좌표 기준 가까운 가게 목록이 거리(km)와 함께 내려오면 정상입니다.

### 3-2. 프론트엔드 (터미널 2)

```bash
cd frontend/api
npm install     # 최초 1회
npm run dev
```

- **http://localhost:5173** 접속.
- `/api` 요청은 Vite dev 서버가 `http://localhost:8080`(백엔드)로 프록시합니다. 따라서 백엔드가 먼저 떠 있어야 합니다.

---

## 4. H2 콘솔 (선택)

브라우저에서 DB를 직접 보고 싶다면:

- 주소: **http://localhost:8080/h2-console**
- JDBC URL: `jdbc:h2:mem:delivery;MODE=MySQL;DB_CLOSE_DELAY=-1`
- 사용자명: `sa`
- 비밀번호: (비움)

> 인메모리라 **백엔드를 재시작하면 데이터가 초기화**되고 `data.sql`로 다시 시드됩니다.

---

## 5. 테스트 / 빌드

```bash
./gradlew :backend:test          # 단위 테스트 (DB 불필요)
./gradlew :backend:build         # 빌드 + 테스트
./gradlew :backend:compileJava   # 컴파일만
```

---

## 6. 참고 — DB 구성에 대해

- 운영용 MySQL/Docker 구성을 걷어내고 **인메모리 H2 (MySQL 호환 모드)** 로 단순화한 강의용 셋업입니다.
- 가게 "내 주변 검색"은 MySQL 공간함수(`ST_Distance_Sphere`) 대신 위/경도 컬럼 기반 **Haversine 식**으로 계산합니다 (`ShopQueryDao`).
- 주문 스냅샷(`ITEMS_SNAPSHOT`)은 `CLOB`(JSON 문자열)로 저장합니다.
