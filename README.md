# SportsTix - Sports Ticketing Platform

MSA Event-Driven sports ticketing system designed for high-concurrency seat booking (100K+ concurrent users).

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Backend | Java 21, Spring Boot 3.3 |
| Database | PostgreSQL 16 (per-service schema), Redis 7 |
| Messaging | Apache Kafka 3.9 (KRaft mode) |
| Frontend | Next.js 14, TypeScript, Tailwind CSS, Zustand |
| Infra | Docker, Kubernetes (EKS), Terraform, ArgoCD |
| Service Mesh | Istio (IngressGateway, mTLS, JWT, routing, rate limit, bot detection) |
| Delivery | Argo Rollouts (canary), KEDA (event-driven autoscaling) |
| Observability | OpenTelemetry, Prometheus, Grafana, Tempo, Loki |
| Resilience | Chaos Mesh, Velero (DR), Kyverno (policy), ESO (secrets) |
| Security | Falco (runtime), Trivy + Cosign (supply chain), cert-manager (TLS) |
| FinOps | OpenCost (cost allocation), Goldilocks (VPA right-sizing) |
| IDP | Backstage (service catalog, Golden Path templates) |
| GitOps | ArgoCD ApplicationSet (multi-env), Notifications (Slack alerts) |
| Testing | JUnit 5, Mockito, Testcontainers, K6 |

## Architecture

```
                    +-----------+
                    | Frontend  |
                    | Next.js   |
                    | :3000     |
                    +-----+-----+
                          |
               +----------v----------+
               | Istio IngressGateway |
               | JWT/Rate/Bot/CORS   |
               +----------+----------+
                          |
     +----------+---------+---------+----------+----------+
     |          |         |         |          |          |
+----v---+ +---v----+ +--v-----+ +-v------+ +-v------+ +-v------+
|  Auth  | |  Game  | | Queue  | |Booking | |Payment | | Admin  |
|  :8081 | |  :8082 | | :8083  | | :8084  | | :8085  | | :8086  |
+---+----+ +---+----+ +---+----+ +---+----+ +---+----+ +---+----+
    |          |           |         |          |          |
    v          v           v         v          v          v
 auth_db    game_db     Redis    booking_db  payment_db  admin_db
                       (Queue)
                          |
                    +-----v-----+
                    |   Kafka   |
                    |  (KRaft)  |
                    +-----------+
```

### Services

| Service | Port | Description | Key Tech |
|---------|------|-------------|----------|
| **Istio IngressGW** | 80/443 | JWT validation, routing, rate limiting, bot detection, CORS | Envoy (C++), EnvoyFilter |
| **Auth Service** | 8081 | Registration, login, JWT token management, JWKS | Spring Security, RS256 |
| **Game Service** | 8082 | Stadium/section/seat CRUD, game scheduling | JPA, Kafka Producer |
| **Queue Service** | 8083 | Virtual waiting room, fair queue ordering | Redis Sorted Set, WebSocket |
| **Booking Service** | 8084 | Seat hold/confirm/cancel with 3-tier locking | jOOQ + JPA Hybrid, SAGA, Resilience4j |
| **Payment Service** | 8085 | Payment processing, refunds (Mock PG) | jOOQ, Kafka Consumer |
| **Admin Service** | 8086 | Dashboard, booking/revenue statistics | Kafka Consumer, Aggregation |

### Communication Patterns

- **Sync REST**: Frontend -> Istio IngressGateway -> Service (reads only)
- **Async Kafka**: All inter-service write operations (no service-to-service REST)
- **WebSocket**: Real-time queue position and seat status updates
- **Data Locality**: Services maintain local replica tables synced via Kafka events

### Key Design Decisions

- **3-Tier Locking** (Booking): Redis distributed lock -> DB pessimistic lock -> Optimistic concurrency
- **SAGA Pattern** (Booking -> Payment): Orchestration-based with compensation on failure
- **jOOQ Hybrid** (Hot Path): Type-safe SQL for seat locking and bulk operations; JPA for CRUD
- **Bot Prevention**: UA fingerprinting, sliding-window rate limiting, CAPTCHA challenge
- **Data Reconciliation**: Scheduled batch to detect/fix stale held seats, booking-seat mismatches

## Getting Started

### Prerequisites

- Docker & Docker Compose
- Java 21 (for IDE support only; builds run in Docker)
- Node.js 18+ (for frontend)

### Infrastructure

```bash
# Start PostgreSQL, Redis, Kafka, Kafka UI
docker compose -f infra/docker-compose.yml up -d
```

### Backend Services

```bash
# Build and run all services
docker compose up --build

# Or run a single service
docker compose up --build booking-service
```

### Frontend

```bash
cd frontend
npm install
npm run dev    # http://localhost:3000
```

### Useful Commands

```bash
# Logs
docker compose logs -f booking-service

# Gradle build (local)
./gradlew build

# Run tests
./gradlew :booking-service:test

# jOOQ codegen
./gradlew :booking-service:generateJooq

# Infrastructure down
docker compose -f infra/docker-compose.yml down
```

## API Documentation

Each service exposes Swagger UI when running:

| Service | Swagger UI |
|---------|-----------|
| Auth | http://localhost:8081/swagger-ui.html |
| Game | http://localhost:8082/swagger-ui.html |
| Queue | http://localhost:8083/swagger-ui.html |
| Booking | http://localhost:8084/swagger-ui.html |
| Payment | http://localhost:8085/swagger-ui.html |
| Admin | http://localhost:8086/swagger-ui.html |

OpenAPI JSON: `http://localhost:{port}/v3/api-docs`

### API Overview

**Auth** - `POST /api/v1/auth/signup`, `POST /api/v1/auth/login`, `POST /api/v1/auth/refresh`, `POST /api/v1/auth/logout`

**Game** - `POST /api/v1/games`, `GET /api/v1/games/{id}`, `GET /api/v1/games`, `GET /api/v1/games/{gameId}/seats`

**Stadium** - `POST /api/v1/stadiums`, `GET /api/v1/stadiums/{id}`, `GET /api/v1/stadiums`

**Queue** - `POST /api/v1/queue/enter`, `GET /api/v1/queue/status`, `DELETE /api/v1/queue/leave`, `POST /api/v1/queue/waiting-room/register`

**Booking** - `POST /api/v1/bookings/hold`, `POST /api/v1/bookings/{id}/confirm`, `POST /api/v1/bookings/{id}/cancel`, `GET /api/v1/bookings/{id}`, `GET /api/v1/bookings`

**Payment** - `POST /api/v1/payments/bookings/{bookingId}/pay`, `POST /api/v1/payments/{id}/refund`, `GET /api/v1/payments/{id}`, `GET /api/v1/payments/bookings/{bookingId}`

**Admin** - `GET /api/v1/admin/dashboard`, `GET /api/v1/admin/games/stats`

### WebSocket Endpoints

| Service | Endpoint | Protocol | Description |
|---------|----------|----------|-------------|
| Queue | `/ws/queue/**` | STOMP | Queue position updates |
| Booking | `/ws/booking/**` | STOMP | Seat status updates |

**Queue STOMP destinations:**
- Send: `/app/queue/status/{gameId}` (header: `X-User-Id`)
- Subscribe: `/user/topic/queue/status`

## Kafka Topics

| Topic | Producer | Consumer | Description |
|-------|----------|----------|-------------|
| `ticket.queue.entered` | Queue | - | User entered queue |
| `ticket.queue.token-issued` | Queue | - | Entrance token issued |
| `ticket.booking.created` | Booking | Admin | Booking created (seats held) |
| `ticket.booking.confirmed` | Booking | Payment, Admin | Booking confirmed |
| `ticket.booking.cancelled` | Booking | Admin | Booking cancelled |
| `ticket.seat.held` | Booking | Game | Seats held |
| `ticket.seat.released` | Booking | Game | Seats released |
| `ticket.payment.completed` | Payment | Booking, Admin | Payment successful |
| `ticket.payment.failed` | Payment | Booking | Payment failed (triggers compensation) |
| `ticket.payment.refunded` | Payment | Admin | Payment refunded |
| `ticket.game.seat-initialized` | Game | Booking | Game seats initialized |
| `ticket.game.info-updated` | Game | Booking | Game info changed |

Dead Letter Topics: `{topic}.DLT` with 3x exponential backoff retry (1s -> 2s -> 4s)

## DevOps & Platform Engineering

### Observability

| Component | Tool | Description |
|-----------|------|-------------|
| Tracing | OpenTelemetry + Tempo | Zero-code Java agent auto-instrumentation, 72h retention |
| Logging | Promtail + Loki | Structured JSON logging (logstash-logback-encoder), trace-log correlation |
| Metrics | Prometheus + Grafana | SLO/SLI dashboards, burn-rate alerts |

### Service Mesh (Istio)

| Plane | Component | Description |
|-------|-----------|-------------|
| **North-South** | IngressGateway | JWT validation (RS256/JWKS), API routing, CORS, rate limiting, bot detection |
| **East-West** | Sidecar (Envoy) | STRICT mTLS, circuit breaker, connection pooling |
| **Control** | Istiod (HA) | priorityClassName: system-cluster-critical, PDB minAvailable: 1 |

**Traffic Flow**: `Client → Istio IngressGateway → Istio Sidecar → Backend`

- **JWT**: RequestAuthentication with JWKS endpoint (`auth:8081/.well-known/jwks.json`)
- **EnvoyFilters**: JWT claim header injection (Lua), local rate limit (50 RPS), bot UA detection (Lua), graceful degradation (JSON 503/429/502/504)
- **Resilience**: Circuit breaker (3x 5xx → 60s ejection, max 30%), retry policy (2 attempts, 3s perTryTimeout)
- **HA**: IngressGateway PDB + pod anti-affinity + zone topology spread, HPA 2-6 replicas
- **WebSocket**: `h2UpgradePolicy: DO_NOT_UPGRADE`, `timeout: 0s` (no retries)

### Progressive Delivery (Argo Rollouts)

- **Canary Deployments**: booking service (Istio VirtualService weight-based)
- **Traffic Splitting**: 20% -> 40% -> 60% -> 80% with analysis gates
- **Automated Analysis**: Prometheus AnalysisTemplates (success-rate >= 95%, p99 < 2s)
- Auto-rollback on metric failure

### Event-Driven Autoscaling (KEDA)

| Service | Trigger | Threshold | Min/Max |
|---------|---------|-----------|---------|
| Booking | Kafka lag (booking.created, seat.held) + CPU | lag > 100, CPU > 65% | 2 / 8 |
| Payment | Kafka lag (payment.completed, booking.confirmed) + CPU | lag > 50, CPU > 70% | 2 / 6 |
| Queue | Redis list length + CPU + Memory | list > 500, CPU > 60% | 2 / 10 |

### Disaster Recovery (Velero)

- **Storage**: S3 (sportstix-velero-backups) + EBS volume snapshots
- **Schedules**: Daily sportstix (02:00 KST), daily observability (03:00 KST), weekly full (Sunday)
- **Retention**: Daily 7 days, weekly 30 days

### Chaos Engineering (Chaos Mesh)

| Experiment | Type | Target | Validates |
|------------|------|--------|-----------|
| booking-pod-kill | PodChaos | booking | PDB + KEDA recovery |
| payment-pod-kill | PodChaos | payment | PDB recovery |
| backend-ingress-latency | NetworkChaos | ingressgateway -> backend | Istio outlier detection |
| booking-egress-latency | NetworkChaos | booking -> DB | HikariCP timeout handling |
| payment-egress-loss | NetworkChaos | payment -> Kafka | Kafka retry + idempotency |
| resilience-validation | Workflow | Multi-step | End-to-end resilience (pod-kill -> health check -> latency -> health check) |

### Security & Compliance

| Area | Tool | Description |
|------|------|-------------|
| Supply Chain | Trivy + Syft + Cosign | Image/fs scan, CycloneDX SBOM, keyless signing (SLSA L2+) |
| Policy-as-Code | Kyverno | 3 Enforce (privileged/root/host) + 4 Audit policies |
| Secrets | External Secrets Operator | AWS Secrets Manager via IRSA |
| SLO/SLI | Prometheus Recording Rules | 99.95% availability, 99.5% latency, 4-tier burn-rate alerts |

### GitOps & Delivery

| Component | Tool | Description |
|-----------|------|-------------|
| Multi-Env | ArgoCD ApplicationSet | goTemplate list generator for dev/staging/prod, conditional autoSync |
| Notifications | ArgoCD Notifications | Slack alerts for sync success/failure/degraded, health changes |
| TLS Automation | cert-manager | Let's Encrypt ClusterIssuer (prod/staging), auto-renewal Certificate |
| Runtime Security | Falco | modern_ebpf driver, custom sportstix rules, Falcosidekick Slack alerts |

### FinOps & Resource Optimization

| Component | Tool | Description |
|-----------|------|-------------|
| Cost Allocation | OpenCost | Per-namespace/service cost analysis, Prometheus integration |
| Right-Sizing | Goldilocks + VPA | Vertical Pod Autoscaler recommendations dashboard |

### Internal Developer Platform (Backstage)

- **Service Catalog**: System, Domain, 7 Components, 6 API definitions
- **Golden Path**: Spring Boot microservice scaffolder template
- **Dependency Graph**: Inter-service relationships and API ownership

### CI/CD Pipeline

```
PR Check:     Gradle build -> Trivy scan -> kubeconform validate
Docker Build: Multi-stage build -> ECR push -> SBOM generate -> Cosign sign
Deploy:       ArgoCD auto-sync -> Argo Rollouts canary -> Prometheus analysis
```

## Project Structure

```
sportstix/
├── common/                  # Shared: ApiResponse, ErrorCode, BaseEntity, Events
├── gateway-service/         # API Gateway - local dev only (K8s: Istio IngressGateway)
├── auth-service/            # Auth (RS256 JWT, JWKS, Spring Security)
├── game-service/            # Game (Stadium, Section, Seat)
├── queue-service/           # Queue (Redis, WebSocket)
├── booking-service/         # Booking (jOOQ + JPA, SAGA, 3-tier Lock)
├── payment-service/         # Payment (Mock PG, jOOQ)
├── admin-service/           # Admin (Dashboard, Stats)
├── frontend/                # Next.js 14
├── infra/
│   ├── docker-compose.yml   # PostgreSQL, Redis, Kafka
│   ├── argocd/              # ArgoCD Applications (Rollouts, KEDA, Velero, Chaos Mesh, Falco, OpenCost, ...)
│   ├── k8s/
│   │   ├── services/        # Deployments / Rollouts
│   │   ├── istio/           # IngressGateway, RequestAuthentication, EnvoyFilter, VirtualService
│   │   ├── rollouts/        # AnalysisTemplates, canary VirtualServices
│   │   ├── keda/            # ScaledObjects, TriggerAuthentication
│   │   ├── velero/           # BackupStorageLocation, Schedules
│   │   ├── chaos/           # PodChaos, NetworkChaos, Workflow, Schedule
│   │   ├── monitoring/      # SLO recording/alerting rules, Grafana dashboards
│   │   └── ...              # configmaps, hpa, pdb, network-policies, rbac
│   └── terraform/           # AWS EKS infrastructure
├── backstage/               # IDP templates (Golden Path scaffolder)
├── k6/                      # Load test scenarios
├── catalog-info.yaml        # Backstage service catalog
├── build.gradle             # Root build config
└── docker-compose.yml       # Service orchestration
```

## License

This project is for portfolio/educational purposes.
