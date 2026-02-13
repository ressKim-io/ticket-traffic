# SportsTix Work Plan - 작업 티켓 순서

> 총 36개 티켓 | P0: 19개 (MVP) | P1: 17개 (Production-ready)
> 예상 총 공수: 190+ hours

## Ticket Completion Protocol (영구 적용)

**모든 티켓은 아래 프로세스를 완료해야 다음 티켓으로 넘어갈 수 있다. 예외 없음.**

```
1. git switch main && git pull origin main          # main 최신화
2. git switch -c feature/TICKET-{N}-description     # 브랜치 생성
3. [구현] 작은 단위 커밋 (파일 1-3개)               # Docker 환경에서 작업
4. [확인] docker compose up --build 로 동작 확인     # Docker에서 실행
5. git push -u origin feature/TICKET-{N}-...        # 푸시
6. /pr-create                                        # PR 자동 생성
7. /backend review (or 상황에 맞는 review)           # 코드 리뷰
8. [수정] Critical/High 이슈 fix 커밋 + push        # 리뷰 반영
9. gh pr merge --squash                              # PR 머지
10. git switch main && git pull origin main          # main pull
11. 다음 티켓으로 이동                                # 1번부터 반복
```

### 리뷰 선택 기준
| 티켓 유형 | 리뷰 명령 |
|-----------|----------|
| Backend (Java/Spring) | `/backend review` |
| Frontend (Next.js) | lint + build check |
| Infra (K8s/Docker) | `/k8s validate` |
| Terraform | `/terraform validate` |

### 리뷰 판정 기준
| Severity | Action | Blocking |
|----------|--------|----------|
| Critical | 즉시 수정, fix 커밋 필수 | YES |
| High | 즉시 수정, fix 커밋 필수 | YES |
| Medium | 가능하면 수정 | NO |
| Low | 기록 후 다음 티켓에서 처리 | NO |

### Docker 원칙
- 모든 인프라(DB, Redis, Kafka)는 `docker compose -f infra/docker-compose.yml`
- 모든 서비스는 Dockerfile + `docker compose`로 빌드/실행
- 로컬 직접 설치 금지

---

## Phase 1: Foundation (Week 1-2)

### TICKET-001: Mono-repo Skeleton [P0, 4h]
- **Branch**: `feature/TICKET-001-monorepo-skeleton`
- **Scope**: Gradle multi-module + Next.js 초기 설정
- **Commits**: settings.gradle → root build.gradle → 각 서비스 모듈 생성 → .gitignore
- **Output**: 빌드 가능한 빈 프로젝트 구조
- **Dependencies**: None (시작점)
- **Done**: push → `/pr-create` → `/backend review` → fix → merge

### TICKET-002: Docker Compose Infra [P0, 2h]
- **Branch**: `feature/TICKET-002-docker-compose`
- **Scope**: PostgreSQL 16, Redis 7, Kafka (KRaft), Kafka UI
- **Commits**: docker-compose.yml → init scripts (DB schema 생성) → .env.example
- **Dependencies**: TICKET-001
- **Done**: push → `/pr-create` → `/backend review` → fix → merge

### TICKET-003: Common Module [P0, 4h]
- **Branch**: `feature/TICKET-003-common-module`
- **Scope**: ApiResponse, ErrorCode, BaseTimeEntity, Event DTOs, Exception Handler
- **Commits**: ApiResponse → ErrorCode enum → BaseTimeEntity → GlobalExceptionHandler → Event DTOs
- **Dependencies**: TICKET-001
- **Done**: push → `/pr-create` → `/backend review` → fix → merge

### TICKET-004: API Gateway [P0, 6h]
- **Branch**: `feature/TICKET-004-api-gateway`
- **Scope**: Spring Cloud Gateway, Route 설정, JWT 검증 필터, Rate Limiting
- **Commits**: Gateway 의존성 → Route config → JwtAuthFilter → RateLimitFilter → CORS config → 테스트
- **Dependencies**: TICKET-001, TICKET-003
- **Done**: push → `/pr-create` → `/backend review` → fix → merge

### TICKET-005: Auth Service [P0, 8h]
- **Branch**: `feature/TICKET-005-auth-service`
- **Scope**: Member Entity, JWT 발급/검증, Login, Signup, Refresh Token
- **Commits**: Member entity → MemberRepository → JwtTokenProvider → AuthService → AuthController → Flyway migration → 테스트
- **Dependencies**: TICKET-003, TICKET-004
- **Done**: push → `/pr-create` → `/backend review` → fix → merge

---

## Phase 2: Core Domain (Week 3-4)

### TICKET-006: Game Service - Stadium/Section/Seat [P0, 6h]
- **Branch**: `feature/TICKET-006-game-stadium-seat`
- **Scope**: Stadium, Section, Seat 엔티티 + CRUD
- **Commits**: Stadium entity → Section entity → Seat entity → Repositories → Service → Controller → Flyway → 테스트
- **Dependencies**: TICKET-003
- **Done**: push → `/pr-create` → `/backend review` → fix → merge

### TICKET-007: Game Service - Game & GameSeat Mapping [P0, 6h]
- **Branch**: `feature/TICKET-007-game-gameseat`
- **Scope**: Game 엔티티, GameSeat 매핑, 좌석 초기화 (25,000석 bulk), Kafka Producer
- **Commits**: Game entity → GameSeat entity → GameSeat bulk init → GameService → GameController → Kafka producer (seat-initialized) → 테스트
- **Dependencies**: TICKET-006
- **Done**: push → `/pr-create` → `/backend review` → fix → merge

---

## Phase 3: Queue System (Week 5-6)

### TICKET-008: Queue Service - Redis Queue [P0, 8h]
- **Branch**: `feature/TICKET-008-queue-redis`
- **Scope**: Redis Sorted Set 기반 대기열, Waiting Room → Queue 전환, Token 발급
- **Commits**: Redis config → QueueService (enter/position/size) → WaitingRoomService → TokenService → QueueController → Scheduler (batch processing) → 테스트
- **Dependencies**: TICKET-002, TICKET-003
- **Done**: push → `/pr-create` → `/backend review` → fix → merge

### TICKET-009: Queue Service - WebSocket Real-time [P0, 6h]
- **Branch**: `feature/TICKET-009-queue-websocket`
- **Scope**: STOMP WebSocket, Redis Pub/Sub 브로드캐스팅, 실시간 순위 업데이트
- **Commits**: WebSocket config → STOMP handler → Redis Pub/Sub listener → Position update broadcaster → 테스트
- **Dependencies**: TICKET-008
- **Done**: push → `/pr-create` → `/backend review` → fix → merge

### TICKET-036: WebSocket Horizontal Scaling [P0, 3h]
- **Branch**: `feature/TICKET-036-websocket-scaling`
- **Scope**: Redis Pub/Sub로 멀티 Pod WebSocket 메시지 동기화
- **Commits**: Redis message listener → WebSocket session registry → Broadcast sync → 테스트
- **Dependencies**: TICKET-009
- **Done**: push → `/pr-create` → `/backend review` → fix → merge

---

## Phase 4: Booking & Payment (Week 7-8)

### TICKET-034: Data Locality - Local Replica Tables [P0, 6h]
- **Branch**: `feature/TICKET-034-data-locality`
- **Scope**: booking_db.local_game_seat, booking_db.local_game_info, payment_db.local_booking_info
- **Commits**: Flyway migration (booking_db) → Flyway migration (payment_db) → Kafka consumer (seat-initialized) → Kafka consumer (game-info) → 동기화 테스트
- **Dependencies**: TICKET-007, TICKET-013
- **Done**: push → `/pr-create` → `/backend review` → fix → merge

### TICKET-035: jOOQ Hybrid Strategy [P0, 5h]
- **Branch**: `feature/TICKET-035-jooq-setup`
- **Scope**: Booking/Payment Service에 jOOQ 설정, Hot Path 쿼리 작성
- **Commits**: jOOQ Gradle plugin → Flyway integration → jOOQ codegen → SeatLockRepository (jOOQ) → LocalGameSeatRepository (jOOQ) → 테스트
- **Dependencies**: TICKET-034
- **Done**: push → `/pr-create` → `/backend review` → fix → merge

### TICKET-010: Booking Service - Seat Booking with 3-tier Lock [P0, 10h]
- **Branch**: `feature/TICKET-010-booking-3tier-lock`
- **Scope**: Redis 분산락 → DB Pessimistic Lock → Optimistic Lock, 5분 Hold TTL
- **Commits**: Booking entity → BookingSeat entity → Redis distributed lock → DB pessimistic lock → Optimistic lock (@Version) → BookingService → BookingController → Hold expiry scheduler → 테스트
- **Dependencies**: TICKET-035, TICKET-008
- **Done**: push → `/pr-create` → `/backend review` → fix → merge

### TICKET-011: Booking Service - SAGA Pattern [P1, 6h]
- **Branch**: `feature/TICKET-011-booking-saga`
- **Scope**: Booking → Payment → Confirm/Compensate 오케스트레이션
- **Commits**: SagaOrchestrator → BookingSagaStep → PaymentSagaStep → CompensationHandler → SagaStateMachine → 테스트
- **Dependencies**: TICKET-010, TICKET-012
- **Done**: push → `/pr-create` → `/backend review` → fix → merge

### TICKET-012: Payment Service - Mock PG [P0, 6h]
- **Branch**: `feature/TICKET-012-payment-service`
- **Scope**: Payment Entity, Mock PG 연동, Kafka Consumer/Producer
- **Commits**: Payment entity → PaymentRepository → Flyway migration → MockPgClient → PaymentService → Kafka consumer (booking.created) → Kafka producer (payment.completed/failed) → 테스트
- **Dependencies**: TICKET-003, TICKET-034
- **Done**: push → `/pr-create` → `/backend review` → fix → merge

### TICKET-013: Kafka Event Pipeline Integration [P1, 6h]
- **Branch**: `feature/TICKET-013-kafka-pipeline`
- **Scope**: 전체 서비스 Kafka 토픽 연결, DLQ 설정, Idempotency
- **Commits**: Kafka config (common) → Topic 정의 → DLQ config → Idempotency key service → Producer/Consumer 연결 (game→booking→payment) → E2E 이벤트 플로우 테스트
- **Dependencies**: TICKET-007, TICKET-010, TICKET-012
- **Done**: push → `/pr-create` → `/backend review` → fix → merge

### TICKET-031: Bot Prevention Layer [P0, 4h]
- **Branch**: `feature/TICKET-031-bot-prevention`
- **Scope**: reCAPTCHA v3, Request Fingerprinting, IP Rate Limiting
- **Commits**: CaptchaValidator → FingerprintFilter → Rate limit config (IP + User) → Gateway integration → 테스트
- **Dependencies**: TICKET-004, TICKET-008
- **Done**: push → `/pr-create` → `/backend review` → fix → merge

---

## Phase 5: Frontend (Week 9-11)

### TICKET-014: Next.js Setup [P0, 4h]
- **Branch**: `feature/TICKET-014-nextjs-setup`
- **Scope**: Next.js 14 App Router, Tailwind CSS, Zustand, TanStack Query, Axios
- **Commits**: create-next-app → Tailwind config → Zustand store setup → API client (Axios) → Layout component → 환경변수 설정
- **Dependencies**: TICKET-004
- **Done**: push → `/pr-create` → lint + build check → fix → merge

### TICKET-015: Auth Pages [P0, 5h]
- **Branch**: `feature/TICKET-015-frontend-auth`
- **Scope**: Login, Signup 페이지, JWT 토큰 관리
- **Commits**: Auth store (Zustand) → Login page → Signup page → Token interceptor → Protected route HOC → 테스트
- **Dependencies**: TICKET-014, TICKET-005
- **Done**: push → `/pr-create` → lint + build check → fix → merge

### TICKET-016: Game List/Detail Pages [P0, 5h]
- **Branch**: `feature/TICKET-016-frontend-games`
- **Scope**: 게임 목록 (SSG/ISR), 게임 상세 (섹션별 잔여석)
- **Commits**: Game list page (SSG) → Game card component → Game detail page → Section availability → Pagination → 테스트
- **Dependencies**: TICKET-014, TICKET-007
- **Done**: push → `/pr-create` → lint + build check → fix → merge

### TICKET-017: Queue Page [P0, 6h]
- **Branch**: `feature/TICKET-017-frontend-queue`
- **Scope**: 대기열 페이지, WebSocket 실시간 순위, 입장 토큰 처리
- **Commits**: WebSocket client → Queue store → Queue page → Position display → Token handler → Auto-redirect on token → 테스트
- **Dependencies**: TICKET-014, TICKET-009
- **Done**: push → `/pr-create` → lint + build check → fix → merge

### TICKET-018: Seat Selection Page [P0, 7h]
- **Branch**: `feature/TICKET-018-frontend-seats`
- **Scope**: 인터랙티브 좌석 맵, 실시간 좌석 상태, 선택/해제
- **Commits**: Seat map component → Section view → Seat status WebSocket → Seat selection logic → Selected seats panel → Cart summary → 테스트
- **Dependencies**: TICKET-017, TICKET-010
- **Done**: push → `/pr-create` → lint + build check → fix → merge

### TICKET-019: Payment/Completion Page [P0, 5h]
- **Branch**: `feature/TICKET-019-frontend-payment`
- **Scope**: 결제 페이지, 카운트다운 타이머 (5분), 결제 완료/실패
- **Commits**: Payment page → Countdown timer → Payment status polling → Success page → Failure/retry page → 테스트
- **Dependencies**: TICKET-018, TICKET-012
- **Done**: push → `/pr-create` → lint + build check → fix → merge

### TICKET-020: My Page [P1, 4h]
- **Branch**: `feature/TICKET-020-frontend-mypage`
- **Scope**: 예매 내역, 예매 상세, 취소 기능
- **Commits**: My page layout → Booking list → Booking detail → Cancel booking → 테스트
- **Dependencies**: TICKET-015, TICKET-019
- **Done**: push → `/pr-create` → lint + build check → fix → merge

---

## Phase 6: Admin (Week 12)

### TICKET-021: Admin Service Backend [P1, 6h]
- **Branch**: `feature/TICKET-021-admin-backend`
- **Scope**: Game CRUD, Stats 집계, Kafka Consumer (이벤트 수집)
- **Commits**: Admin entities → Stats aggregation → Game management API → Kafka consumers → Dashboard API → 테스트
- **Dependencies**: TICKET-013
- **Done**: push → `/pr-create` → `/backend review` → fix → merge

### TICKET-022: Admin Frontend [P1, 5h]
- **Branch**: `feature/TICKET-022-admin-frontend`
- **Scope**: Admin Dashboard, 차트 (Recharts), 게임 관리 UI
- **Commits**: Admin layout → Dashboard page → Charts (booking stats) → Game management CRUD → 테스트
- **Dependencies**: TICKET-021, TICKET-014
- **Done**: push → `/pr-create` → lint + build check → fix → merge

---

## Phase 7: Infrastructure (Week 13-14)

### TICKET-023: Dockerfiles [P1, 4h]
- **Branch**: `feature/TICKET-023-dockerfiles`
- **Scope**: Multi-stage Dockerfile (각 서비스 + Frontend)
- **Commits**: Base Dockerfile (Java) → 각 서비스 Dockerfile → Frontend Dockerfile → docker-compose.prod.yml → 테스트
- **Dependencies**: TICKET-019
- **Done**: push → `/pr-create` → `/backend review` → fix → merge

### TICKET-024: Kubernetes Manifests [P1, 6h]
- **Branch**: `feature/TICKET-024-k8s-manifests`
- **Scope**: Deployment, Service, ConfigMap, Secret, HPA
- **Commits**: Namespace → ConfigMaps → Secrets → 각 서비스 Deployment+Service → HPA → PDB
- **Dependencies**: TICKET-023
- **Done**: push → `/pr-create` → `/backend review` → fix → merge

### TICKET-025: K8s Ingress + Network Policy [P1, 4h]
- **Branch**: `feature/TICKET-025-k8s-network`
- **Scope**: Ingress Controller, TLS, NetworkPolicy (서비스 간 격리)
- **Commits**: Ingress → TLS config → NetworkPolicy (per service) → 테스트
- **Dependencies**: TICKET-024
- **Done**: push → `/pr-create` → `/backend review` → fix → merge

### TICKET-026: CI/CD Pipeline [P1, 8h]
- **Branch**: `feature/TICKET-026-cicd`
- **Scope**: GitHub Actions + ArgoCD
- **Commits**: PR workflow (lint+test+build) → Docker build workflow → ECR push → ArgoCD app manifests → Rollback strategy
- **Dependencies**: TICKET-024, TICKET-023
- **Done**: push → `/pr-create` → `/backend review` → fix → merge

### TICKET-027: Monitoring [P1, 6h]
- **Branch**: `feature/TICKET-027-monitoring`
- **Scope**: Prometheus + Grafana + Spring Actuator
- **Commits**: Actuator config → Prometheus ServiceMonitor → Grafana dashboards → Alert rules → 테스트
- **Dependencies**: TICKET-024
- **Done**: push → `/pr-create` → `/backend review` → fix → merge

### TICKET-032: Circuit Breaker [P1, 3h]
- **Branch**: `feature/TICKET-032-circuit-breaker`
- **Scope**: Resilience4j - Circuit Breaker, Bulkhead, Retry
- **Commits**: Resilience4j config → Payment circuit breaker → Fallback handlers → 테스트
- **Dependencies**: TICKET-011
- **Done**: push → `/pr-create` → `/backend review` → fix → merge

---

## Phase 8: Quality & Documentation (Week 15+)

### TICKET-028: Load Testing [P1, 8h]
- **Branch**: `feature/TICKET-028-load-testing`
- **Scope**: K6 시나리오 (대기열 → 예매 → 결제 Full Flow)
- **Commits**: K6 setup → Queue scenario → Booking scenario → Payment scenario → Full flow → 결과 분석
- **Dependencies**: TICKET-019, TICKET-024
- **Done**: push → `/pr-create` → `/backend review` → fix → merge

### TICKET-029: Test Coverage [P1, 10h]
- **Branch**: `feature/TICKET-029-test-coverage`
- **Scope**: Unit, Integration, Concurrency 테스트 보강
- **Commits**: 각 서비스별 unit test 보강 → Integration test (Testcontainers) → Concurrency test (좌석 동시 점유) → JaCoCo report
- **Dependencies**: 모든 서비스 완료 후
- **Done**: push → `/pr-create` → `/backend review` → fix → merge

### TICKET-033: Data Reconciliation Batch [P1, 4h]
- **Branch**: `feature/TICKET-033-data-reconciliation`
- **Scope**: Local replica ↔ Source table 정합성 검증 배치
- **Commits**: Reconciliation scheduler → Game seat consistency check → Booking consistency check → Alert on mismatch → 테스트
- **Dependencies**: TICKET-034
- **Done**: push → `/pr-create` → `/backend review` → fix → merge

### TICKET-030: API Docs + README [P1, 4h]
- **Branch**: `feature/TICKET-030-api-docs`
- **Scope**: SpringDoc OpenAPI, README 정비
- **Commits**: SpringDoc 설정 → 각 Controller @Operation 추가 → README 업데이트 → 아키텍처 다이어그램
- **Dependencies**: 모든 서비스 완료 후
- **Done**: push → `/pr-create` → `/backend review` → fix → merge

---

## Execution Order (Recommended Critical Path)

```
Phase 1 (Foundation):
  TICKET-001 → TICKET-002 (parallel with TICKET-003)
  TICKET-003 → TICKET-004 → TICKET-005

Phase 2 (Core Domain):
  TICKET-006 → TICKET-007

Phase 3 (Queue):
  TICKET-008 → TICKET-009 → TICKET-036

Phase 4 (Booking & Payment) - CRITICAL PATH:
  TICKET-013 → TICKET-034 → TICKET-035 → TICKET-010
  TICKET-012 (parallel with TICKET-034)
  TICKET-010 + TICKET-012 → TICKET-011
  TICKET-031 (parallel, after TICKET-004)

Phase 5 (Frontend):
  TICKET-014 → TICKET-015, TICKET-016 (parallel)
  TICKET-016 → TICKET-017 → TICKET-018 → TICKET-019 → TICKET-020

Phase 6 (Admin):
  TICKET-021 → TICKET-022

Phase 7 (Infra):
  TICKET-023 → TICKET-024 → TICKET-025 (parallel with TICKET-026)
  TICKET-027, TICKET-032 (parallel)

Phase 8 (Quality):
  TICKET-028, TICKET-029, TICKET-033, TICKET-030 (parallel where possible)
```

---

## Commit Frequency Guide

각 티켓 내에서 다음 단위로 커밋:
1. **Entity/Domain** - 엔티티 클래스 1-2개당 1 커밋
2. **Repository** - Repository 인터페이스당 1 커밋
3. **Service** - 서비스 로직 주요 메서드당 1 커밋
4. **Controller** - Controller + DTO 1 커밋
5. **Config** - 설정 파일당 1 커밋
6. **Test** - 테스트 클래스당 1 커밋
7. **Migration** - Flyway 파일당 1 커밋

예시 (TICKET-005 Auth Service):
```
feat(auth): add Member entity and Flyway migration V1
feat(auth): add MemberRepository with findByEmail
feat(auth): add JwtTokenProvider with RS256
feat(auth): add RefreshToken entity and repository
feat(auth): add AuthService login/signup/refresh
feat(auth): add AuthController REST endpoints
feat(auth): add auth DTOs (LoginRequest, SignupRequest, TokenResponse)
test(auth): add JwtTokenProvider unit tests
test(auth): add AuthService unit tests
test(auth): add AuthController integration tests
```
