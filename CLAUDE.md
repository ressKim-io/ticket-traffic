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

1. **No Secrets in Code** - Use environment variables or secret managers
2. **Frequent Commits** - Commit every logical unit of work (function, config, test)
3. **Test Coverage** - Minimum 80%, all new features must include tests
4. **No N+1 Queries** - Use fetch join/@EntityGraph or jOOQ
5. **Data Locality** - Services must NOT call each other via REST for writes. Use Kafka events.
6. **Docker First** - 모든 것은 Docker 위에서 실행한다. 로컬 설치 의존 금지.

## Docker Environment (필수)

모든 인프라와 서비스는 Docker Compose 위에서 실행한다.

### 인프라 (항상 Docker)
```bash
docker compose -f infra/docker-compose.yml up -d   # PostgreSQL, Redis, Kafka, Kafka UI
```

### 개발 서비스 (Docker 빌드 & 실행)
```bash
docker compose -f docker-compose.yml up --build     # 전체 서비스 빌드 + 실행
docker compose -f docker-compose.yml up --build {service}  # 개별 서비스
```

### 원칙
- **로컬에 PostgreSQL, Redis, Kafka 설치 금지** → Docker Compose로만 실행
- 각 서비스는 Dockerfile을 가지며, `docker compose`로 빌드/실행
- Gradle 빌드도 Docker 멀티스테이지 빌드 안에서 수행
- 환경변수는 `.env` 파일 + `docker-compose.yml`의 environment로 관리
- `docker compose logs -f {service}` 로 로그 확인

## Ticket Workflow (필수 프로세스)

**이 워크플로우는 모든 티켓에 영구 적용된다. 예외 없음.**

### 1. Branch 생성 (main에서 분기)
```bash
git switch main
git pull origin main
git switch -c feature/TICKET-{number}-description
```

### 2. 구현 + 작은 단위 커밋
- 파일 1-3개 변경될 때마다 즉시 커밋
- 설정, 엔티티, 서비스, 컨트롤러, 테스트 각각 별도 커밋
- Docker 환경에서 동작 확인 후 커밋

### 3. PR 생성
```bash
git push -u origin feature/TICKET-{number}-description
```
→ `/pr-create` 실행하여 PR 자동 생성

### 4. 리뷰
- Backend 티켓: `/backend review` 실행
- Frontend 티켓: `/java review` 대신 lint + build 확인
- Infra 티켓: `/k8s validate` 또는 `/terraform validate` 실행
- **Critical/High 이슈 → 반드시 수정 커밋 후 push**

### 5. Merge → main Pull → 다음 티켓
```bash
gh pr merge --squash                          # PR 머지 (squash)
git switch main
git pull origin main                          # main 최신화
git switch -c feature/TICKET-{next}-...       # 다음 티켓 시작
```

### 전체 흐름 요약
```
main pull → branch 생성 → 구현(작은 커밋) → push → PR 생성 → review → fix → merge → main pull → 반복
```

> **절대 규칙**: PR + review 없이 다음 티켓으로 넘어가지 않는다.

## Git Conventions

### Commit Strategy (촘촘한 커밋)
- 파일 1-3개 변경될 때마다 커밋
- 설정 파일, 엔티티, 서비스 로직, 테스트를 각각 별도 커밋
- 예시: `feat(auth): add Member entity` → `feat(auth): add MemberRepository` → `feat(auth): add AuthService` → `test(auth): add AuthService unit tests`

### Commit Format
```
<type>(<scope>): <subject>
```
- **Types**: feat, fix, docs, style, refactor, test, chore
- **Scopes**: common, gateway, auth, game, queue, booking, payment, admin, frontend, infra, k8s, ci

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
- `/spring-data` - JPA, QueryDSL patterns (Cold Path)
- `/jooq-hybrid` - jOOQ + JPA hybrid strategy (Hot Path: 좌석 락킹, Bulk, Local Replica)
- `/spring-cache` - Redis caching strategy
- `/spring-security` - Security config, JWT
- `/spring-oauth2` - OAuth2, JWT token issuance
- `/spring-testing` - JUnit, Mockito
- `/spring-testcontainers` - Integration tests with Testcontainers
- `/concurrency-spring` - Locking, deadlock prevention
- `/distributed-lock` - Redis/Redisson distributed lock
- `/msa-saga` - SAGA orchestration pattern
- `/msa-event-driven` - Event-driven architecture
- `/msa-resilience` - Circuit Breaker, Bulkhead
- `/high-traffic-design` - High concurrency system design
- `/kafka` - Kafka producer/consumer patterns
- `/kafka-patterns` - Advanced Kafka patterns (DLQ, idempotency)
- `/redis-streams` - Redis data structures and patterns
- `/api-design` - REST API design (RFC 9457 errors)
- `/docker` - Multi-stage Dockerfile optimization
- `/database` - Index, query optimization
- `/database-migration` - Flyway migration strategy
- `/k8s-autoscaling` - HPA, VPA configuration
- `/observability` - Logging, metrics (RED Method)
- `/load-testing` - K6/Gatling load test
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
