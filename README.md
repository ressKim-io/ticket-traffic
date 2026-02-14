# SportsTix - Sports Ticketing Platform

MSA Event-Driven sports ticketing system designed for high-concurrency seat booking (100K+ concurrent users).

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Backend | Java 21, Spring Boot 3.3, Spring Cloud Gateway |
| Database | PostgreSQL 16 (per-service schema), Redis 7 |
| Messaging | Apache Kafka 3.9 (KRaft mode) |
| Frontend | Next.js 14, TypeScript, Tailwind CSS, Zustand |
| Infra | Docker, Kubernetes (EKS), ArgoCD, Terraform |
| Testing | JUnit 5, Mockito, Testcontainers, K6 |

## Architecture

```
                    +-----------+
                    | Frontend  |
                    | Next.js   |
                    | :3000     |
                    +-----+-----+
                          |
                    +-----v-----+
                    |  Gateway   |
                    |  :8080     |
                    |  JWT/Rate  |
                    +-----+-----+
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
| **API Gateway** | 8080 | Routing, JWT validation, rate limiting, bot prevention | Spring Cloud Gateway (WebFlux) |
| **Auth Service** | 8081 | Registration, login, JWT token management | Spring Security, BCrypt |
| **Game Service** | 8082 | Stadium/section/seat CRUD, game scheduling | JPA, Kafka Producer |
| **Queue Service** | 8083 | Virtual waiting room, fair queue ordering | Redis Sorted Set, WebSocket |
| **Booking Service** | 8084 | Seat hold/confirm/cancel with 3-tier locking | jOOQ + JPA Hybrid, SAGA, Resilience4j |
| **Payment Service** | 8085 | Payment processing, refunds (Mock PG) | jOOQ, Kafka Consumer |
| **Admin Service** | 8086 | Dashboard, booking/revenue statistics | Kafka Consumer, Aggregation |

### Communication Patterns

- **Sync REST**: Frontend -> Gateway -> Service (reads only)
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

**Game** - `POST /api/v1/games`, `GET /api/v1/games/{id}`, `GET /api/v1/games`, `POST /api/v1/games/{id}/initialize-seats`

**Stadium** - `POST /api/v1/stadiums`, `GET /api/v1/stadiums/{id}`, `GET /api/v1/stadiums`

**Queue** - `POST /api/v1/queue/enter`, `GET /api/v1/queue/status`, `DELETE /api/v1/queue/leave`, `POST /api/v1/queue/waiting-room/register`

**Booking** - `POST /api/v1/bookings/hold`, `POST /api/v1/bookings/{id}/confirm`, `POST /api/v1/bookings/{id}/cancel`, `GET /api/v1/bookings/{id}`, `GET /api/v1/bookings`

**Payment** - `POST /api/v1/payments/bookings/{bookingId}/pay`, `POST /api/v1/payments/{id}/refund`, `GET /api/v1/payments/{id}`, `GET /api/v1/payments/bookings/{bookingId}`

**Admin** - `GET /api/v1/admin/dashboard`, `GET /api/v1/admin/games/stats`

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

## Project Structure

```
sportstix/
├── common/                  # Shared: ApiResponse, ErrorCode, BaseEntity, Events
├── gateway-service/         # API Gateway (WebFlux)
├── auth-service/            # Auth (JWT, Spring Security)
├── game-service/            # Game (Stadium, Section, Seat)
├── queue-service/           # Queue (Redis, WebSocket)
├── booking-service/         # Booking (jOOQ + JPA, SAGA, 3-tier Lock)
├── payment-service/         # Payment (Mock PG, jOOQ)
├── admin-service/           # Admin (Dashboard, Stats)
├── frontend/                # Next.js 14
├── infra/
│   ├── docker-compose.yml   # PostgreSQL, Redis, Kafka
│   ├── k8s/                 # Kubernetes manifests
│   └── terraform/           # AWS EKS infrastructure
├── k6/                      # Load test scenarios
├── build.gradle             # Root build config
└── docker-compose.yml       # Service orchestration
```

## License

This project is for portfolio/educational purposes.
