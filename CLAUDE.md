# SportsTix - Sports Ticketing Platform

## Project Overview
- **Type**: MSA (Microservice Architecture) Event-Driven Sports Ticketing System
- **Tech Stack**: Java 21, Spring Boot 3.3+, PostgreSQL 16, Redis 7, Kafka 3.x (KRaft), Next.js 14+
- **Architecture**: 6 Microservices + API Gateway + Frontend

## Language
- Response: 한국어
- Code comments: English
- Commit messages: English (Conventional Commits)

## CRITICAL Rules

1. **MANDATORY WORKFLOW** - 아래 "Fixed Routine"을 반드시 따른다. 단 한 단계도 건너뛸 수 없다.
2. **No Secrets in Code** - Use environment variables or secret managers
3. **Frequent Commits** - Commit every logical unit of work (function, config, test)
4. **Test Coverage** - Minimum 80%, all new features must include tests
5. **No N+1 Queries** - Use fetch join/@EntityGraph or jOOQ
6. **Data Locality** - Services must NOT call each other via REST for writes. Use Kafka events.
7. **Docker First** - 모든 것은 Docker 위에서 실행한다. 로컬 설치 의존 금지.

## Development Environment

두 가지 실행 모드를 지원한다. 상황에 맞게 선택.

### Mode 1: Docker Compose (기본)

모든 인프라와 서비스를 Docker Compose로 실행한다.

```bash
docker compose -f infra/docker-compose.yml up -d        # 인프라: PostgreSQL, Redis, Kafka, Kafka UI
docker compose -f docker-compose.yml up --build          # 전체 서비스 빌드 + 실행
docker compose -f docker-compose.yml up --build {service}  # 개별 서비스
docker compose logs -f {service}                         # 로그 확인
```

### Mode 2: k3d (Kubernetes 로컬 배포)

k3d 클러스터에서 K8s 매니페스트 기반으로 전체 서비스를 배포한다.
PostgreSQL/Redis는 로컬 systemd, Kafka와 앱 서비스는 K8s Pod로 실행.

```bash
./scripts/k3d-local-setup.sh              # 최초 전체 설정 (클러스터 생성 ~ Pod 배포)
./scripts/k3d-local-setup.sh restart      # 재부팅 후 복구 (iptables + Pod 재시작)
./scripts/k3d-local-setup.sh build        # 이미지만 재빌드 + k3d import
./scripts/k3d-local-setup.sh iptables     # iptables 규칙만 재적용
```

**k3d 주의사항**:
- 재부팅 시 iptables 규칙이 초기화됨 → `restart` 명령으로 복구
- Bitnami Helm 차트 유료화 → apache/kafka 공식 이미지 직접 사용
- Kafka probe는 tcpSocket만 사용 (JVM CLI probe는 CrashLoop 유발)
- 트러블슈팅 로그: `docs/dev-logs/2026-02-27-k3d-local-deploy-troubleshooting.md`

### 원칙
- 각 서비스는 Dockerfile을 가지며 Docker 멀티스테이지 빌드로 수행
- 환경변수는 `.env` 파일 + `docker-compose.yml` 또는 K8s ConfigMap/Secret으로 관리

## Fixed Routine (절대 루틴)

**이 루틴은 모든 작업에 무조건 적용된다. 예외 없음. 단계를 건너뛰거나 순서를 바꿀 수 없다.**

```
STEP 1 → 2 → 3 → 4 → 5 → 6 → 7 (순서 고정, 스킵 불가)
```

---

### STEP 1. Branch 생성
> 반드시 main에서 최신 상태로 분기한다.

```bash
git switch main
git pull origin main
git switch -c feature/{issue-number}-description
```

---

### STEP 2. 구현 + 촘촘한 커밋
> 파일 1-3개 변경될 때마다 즉시 커밋한다. 한 번에 몰아서 커밋 금지.

**커밋 단위 (각각 별도 커밋)**:
- 설정 파일 (build.gradle, application.yml, docker-compose 등)
- Entity / Domain 클래스
- Repository / DAO
- Service 로직
- Controller / API
- Test 코드
- Infra / K8s manifest

**커밋 포맷**: `<type>(<scope>): <subject>`
- Types: `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`
- Scopes: `common`, `gateway`, `auth`, `game`, `queue`, `booking`, `payment`, `admin`, `frontend`, `infra`, `k8s`, `ci`
- 예: `feat(auth): add Member entity` → `feat(auth): add MemberRepository` → `test(auth): add AuthService unit tests`

---

### STEP 3. Push
```bash
git push -u origin feature/{issue-number}-description
```

---

### STEP 4. PR 생성
> `/dx:pr-create` 실행하여 PR 자동 생성. body에 `Closes #{issue-number}` 포함.

---

### STEP 5. PR Code Review (핵심 단계)
> PR 생성 직후, 변경 유형에 맞는 코드 리뷰를 실행하고 **결과를 PR 댓글로 기록**한다.

**리뷰 실행 (변경 유형별)**:

| 작업 유형 | 리뷰 명령어 |
|-----------|-------------|
| Backend (Java/Spring) | `/backend:review` |
| Frontend (Next.js) | `/frontend-review` 또는 lint + build 확인 |
| Infra (K8s) | `/k8s:validate` |
| Infra (Terraform) | `/terraform:validate` |

- 변경된 파일 유형에 따라 해당하는 리뷰를 **모두** 실행
- 예: Backend + K8s 동시 변경 시 → `/backend:review` + `/k8s:validate` 둘 다

**리뷰 결과를 PR 댓글로 게시**:
```bash
gh pr comment {number} --body "리뷰 결과 markdown"
```

댓글 형식:
```markdown
## Code Review — {review-type}

### Summary
- **Critical**: {count}
- **High**: {count}
- **Medium**: {count}
- **Low**: {count}

### Critical Issues
{없으면 "None" / 있으면 파일:라인 + 설명 + 수정 방안}

### High Issues
{없으면 "None" / 있으면 파일:라인 + 설명 + 수정 방안}

### Medium/Low Issues
{요약 또는 상세}

### Verdict
{✅ PASS — Critical/High 0건 | ❌ FAIL — 수정 필요}
```

**Critical/High 이슈 발견 시**: 수정 커밋 → push → 리뷰 재실행 → PR 댓글 업데이트.
Critical/High가 0건이 될 때까지 STEP 6으로 넘어갈 수 없다.

---

### STEP 6. CI 통과 + Review 확인 → Merge
> CI가 통과하고, Code Review Verdict가 ✅ PASS일 때만 merge한다.

```bash
gh pr checks {pr-number} --watch              # CI 통과 대기
gh pr view {pr-number} --comments             # Review 댓글 Verdict 확인
gh pr merge {pr-number} --squash --delete-branch  # squash merge + branch 정리
```

**CI 실패 시**: 원인 파악 → 수정 커밋 → push → CI 재확인 → 통과 후 merge.

---

### STEP 7. main 복귀 + Pull
> merge 완료 후 반드시 main으로 돌아와 최신화한다. 여기까지가 1사이클.

```bash
git switch main
git pull origin main
```

**이 단계를 완료해야 다음 작업을 시작할 수 있다.**

---

### 전체 사이클 요약
```
┌─────────────────────────────────────────────────────────────────┐
│  STEP 1. branch 생성 (main에서)                                  │
│  STEP 2. 구현 + 촘촘한 커밋 (1-3파일마다)                         │
│  STEP 3. push                                                    │
│  STEP 4. PR 생성 (/dx:pr-create)                                 │
│  STEP 5. PR Code Review → PR 댓글 기록 (Critical/High 0건까지)   │
│  STEP 6. CI 통과 + Review PASS → merge (--squash --delete-branch)│
│  STEP 7. main 복귀 + pull                                        │
│  ─────────── 1 사이클 완료. 다음 작업 시작 가능 ──────────────    │
└─────────────────────────────────────────────────────────────────┘
```

> **위반 불가**: 어떤 상황에서도 이 순서를 지킨다. "나중에 하겠다", "이번만 건너뛰겠다"는 없다.

## Git Conventions

### Branch Naming
```
feature/TICKET-{number}-description
fix/TICKET-{number}-description
```

## Services & Ports

| Service | Port | DB Schema | Key Tech |
|---------|------|-----------|----------|
| API Gateway | 8080 | - | Spring Cloud Gateway, JWT, Rate Limit |
| Auth Service | 8081 | auth_db | JWT, Spring Security |
| Game Service | 8082 | game_db | JPA + QueryDSL |
| Queue Service | 8083 | Redis only | Redis Sorted Set, WebSocket |
| Booking Service | 8084 | booking_db | jOOQ + JPA, SAGA, 3-tier Lock |
| Payment Service | 8085 | payment_db | Mock PG, Kafka Consumer |
| Admin Service | 8086 | admin_db | Dashboard, Stats |
| Frontend | 3000 | - | Next.js 14, Tailwind, Zustand |

## Project Structure
```
sportstix/
├── common/                    # Shared: ApiResponse, ErrorCode, BaseEntity, Events
├── gateway/                   # API Gateway (Port 8080)
├── auth-service/              # Auth (Port 8081)
├── game-service/              # Game (Port 8082)
├── queue-service/             # Queue (Port 8083)
├── booking-service/           # Booking (Port 8084) - jOOQ + JPA Hybrid
├── payment-service/           # Payment (Port 8085)
├── admin-service/             # Admin (Port 8086)
├── frontend/                  # Next.js (Port 3000)
├── infra/
│   ├── docker-compose.yml     # PostgreSQL, Redis, Kafka, Kafka UI
│   ├── k8s/                   # Kubernetes manifests
│   └── terraform/             # AWS EKS infrastructure
├── build.gradle               # Root build config
├── settings.gradle
└── tmp/                       # Design documents (reference only)
```

### Service Package Convention
```
com.sportstix.{service}/
├── controller/        # REST endpoints
├── service/           # Business logic
├── domain/            # JPA Entities
├── repository/        # JPA/jOOQ repositories
├── jooq/              # jOOQ repos (Hot Path only: booking, payment)
├── event/
│   ├── producer/      # Kafka publishers
│   └── consumer/      # Kafka listeners
├── websocket/         # WebSocket handlers (queue, booking)
├── config/            # Spring configs
├── dto/request/       # Request DTOs
├── dto/response/      # Response DTOs
└── mapper/            # DTO <-> Entity conversion
```

## Communication Patterns
- **Sync REST**: Frontend → Gateway → Service (read operations ONLY)
- **Async Kafka**: ALL inter-service write operations
- **WebSocket**: Real-time updates (queue position, seat status)
- **NO service-to-service REST calls**

## Data Strategy
- **Cold Path (CRUD)**: JPA + QueryDSL
- **Hot Path (High Concurrency)**: jOOQ for type-safe SQL
  - Seat locking/status changes
  - Bulk operations (25,000 seats init)
  - Local replica table queries
- **Data Locality**: Services maintain local replica tables synced via Kafka

## Key Redis Patterns
| Key | Type | TTL | Usage |
|-----|------|-----|-------|
| `queue:{gameId}` | Sorted Set | Game end | Queue position |
| `waitingroom:{gameId}` | Set | Open time | Pre-registration |
| `queue:token:{gameId}:{userId}` | String | 600s | Entrance token |
| `lock:seat:{gameSeatId}` | String | 5s | Distributed lock |
| `ws:broadcast:{channel}` | Pub/Sub | - | WebSocket cross-pod |

## Kafka Topics
- `ticket.queue.entered`, `ticket.queue.token-issued`
- `ticket.booking.created`, `ticket.booking.confirmed`, `ticket.booking.cancelled`
- `ticket.seat.held`, `ticket.seat.released`
- `ticket.payment.completed`, `ticket.payment.failed`, `ticket.payment.refunded`
- `ticket.game.seat-initialized`, `ticket.game.info-updated`
- DLT: `{topic}.DLT` | Retry: 3x exponential backoff (1s → 2s → 4s)

## Quick Commands
- Infra up: `docker compose -f infra/docker-compose.yml up -d`
- Infra down: `docker compose -f infra/docker-compose.yml down`
- All services: `docker compose up --build`
- Single service: `docker compose up --build {service-name}`
- Logs: `docker compose logs -f {service-name}`
- Build (in Docker): `docker compose build {service-name}`
- Test (Gradle): `./gradlew :{service}:test`
- Full build: `./gradlew build`
- jOOQ codegen: `./gradlew :booking-service:generateJooq`

## Skills Reference

### Spring / Java
- `/spring-data` - JPA, QueryDSL patterns (Cold Path)
- `/jooq-hybrid` - jOOQ + JPA hybrid strategy (Hot Path: 좌석 락킹, Bulk, Local Replica)
- `/spring-cache` - Redis caching strategy
- `/spring-security` - Security config, JWT
- `/spring-oauth2` - OAuth2, JWT token issuance
- `/spring-testing` - JUnit, Mockito
- `/spring-testcontainers` - Integration tests with Testcontainers
- `/concurrency-spring` - Locking, deadlock prevention

### MSA Patterns
- `/distributed-lock` - Redis/Redisson distributed lock
- `/msa-saga` - SAGA orchestration pattern
- `/msa-event-driven` - Event-driven architecture
- `/msa-resilience` - Circuit Breaker, Bulkhead
- `/high-traffic-design` - High concurrency system design
- `/api-design` - REST API design (RFC 9457 errors)

### Kafka (심화)
- `/kafka` - Kafka producer/consumer patterns
- `/kafka-patterns` - Advanced Kafka patterns (DLQ, idempotency)
- `/kafka-advanced` - Transactional API, Exactly-Once, KIP-848 리밸런싱, Offset 전략
- `/kafka-streams` - KTable, Windowing, Interactive Queries, Stateful Processing
- `/kafka-connect-cdc` - Debezium CDC, Outbox Event Router, Schema Registry

### Istio Service Mesh
- `/istio-core` - Istio 핵심 개념 (Sidecar vs Ambient)
- `/istio-gateway` - Gateway API vs Istio Gateway 비교 허브
- `/istio-gateway-classic` - Istio Gateway + VirtualService 라우팅
- `/istio-gateway-api` - K8s Gateway API + HTTPRoute 라우팅
- `/istio-security` - mTLS, PeerAuthentication, AuthorizationPolicy, JWT
- `/istio-advanced-traffic` - Fault Injection, Traffic Mirroring, Canary, JWT Claim 라우팅
- `/istio-metrics` - Prometheus 연동, ServiceMonitor, RED 메트릭
- `/istio-observability` - 모니터링 통합 허브 (메트릭, 트레이싱, Kiali)
- `/istio-otel` - OpenTelemetry 통합, Telemetry API, W3C Trace Context
- `/istio-tracing` - Jaeger/Tempo 분산 트레이싱
- `/istio-kiali` - Kiali 서비스 토폴로지 시각화
- `/istio-ext-authz` - External Authorization (OPA, 외부 인증 서버)
- `/istio-ambient` - Ambient Mode (ztunnel, Waypoint)
- `/istio-multicluster` - Multi-Primary, East-West Gateway

### Kubernetes
- `/k8s-autoscaling` - HPA, VPA configuration
- `/k8s-autoscaling-advanced` - Karpenter, 조합 전략, 모니터링
- `/k8s-scheduling-advanced` - 실전 시나리오, Topology Spread, 디버깅
- `/k8s-traffic` - 트래픽 제어 허브 (Rate Limiting, Circuit Breaker)
- `/k8s-traffic-istio` - Istio Rate Limiting & Circuit Breaker
- `/k8s-traffic-ingress` - NGINX Ingress Rate Limiting
- `/gateway-api` - K8s Gateway API (Ingress 후속)
- `/gateway-api-migration` - Ingress → Gateway API 마이그레이션

### Infrastructure
- `/redis-streams` - Redis data structures and patterns
- `/docker` - Multi-stage Dockerfile optimization
- `/database` - Index, query optimization
- `/database-migration` - Flyway migration strategy

### Observability
- `/observability` - Logging, metrics (RED Method)
- `/load-testing` - K6/Gatling load test

### Frontend
- `/nextjs-app-router` - Next.js 14 App Router (Server/Client Components, Streaming)
- `/react-components` - React 19 patterns (useActionState, useOptimistic, WebSocket)
- `/zustand-state` - Zustand state management with Next.js 14
- `/tanstack-query` - TanStack Query (prefetch, hydration, mutation)
- `/tailwind-patterns` - Tailwind CSS + CVA component variants
- `/frontend-testing` - Vitest + React Testing Library + Playwright
- `/frontend-review` - Frontend code review checklist

## Agents
- `@java-expert` - Java 21 + Spring Boot 3.3 patterns
- `@database-expert` - PostgreSQL optimization, jOOQ
- `@redis-expert` - Redis data structures, distributed lock
- `@ticketing-expert` - Ticketing domain (queue, seat locking, booking flow)
- `@architect-agent` - Architecture decisions, MSA patterns
- `@saga-agent` - SAGA pattern implementation
- `@code-reviewer` - Code review
- `@anti-bot` - Bot prevention, rate limiting, CAPTCHA
- `@load-tester` - Performance testing
- `@k8s-troubleshooter` - Kubernetes debugging
- `@ci-optimizer` - CI/CD pipeline optimization
- `@frontend-expert` - Next.js 14 + React 19, Zustand, TanStack Query, Tailwind
- `@otel-expert` - OpenTelemetry 대규모 트래픽 설정 (Istio + OTel 통합)
- `@incident-responder` - Production incident triage, RCA, guided remediation
- `@security-scanner` - Security vulnerability scanning, misconfiguration detection

## Design Documents (Reference)
All design documents are in `tmp/` directory:
- `01-PRD` - Product Requirements
- `02-시스템아키텍처` - System Architecture (arc42, C4)
- `03-ERD` - Database Schema (5 schemas + Redis)
- `04-API명세` - API Specifications
- `05-작업티켓` - Work Tickets (36 tickets)
- `06-시퀀스다이어그램` - Sequence Diagrams
- `07-인프라설계서` - Infrastructure (AWS EKS, Terraform)
- `08-프로젝트구조` - Project Structure & Conventions

---
*See `.claude/TICKETS.md` for detailed work plan*
