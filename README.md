# SportsTix - Sports Ticketing Platform

MSA Event-Driven 스포츠 티켓팅 시스템

## Tech Stack
- **Backend**: Java 21, Spring Boot 3.3+, PostgreSQL 16, Redis 7, Kafka 3.x
- **Frontend**: Next.js 14+, TypeScript, Tailwind CSS
- **Infra**: Docker, Kubernetes (EKS), ArgoCD, Terraform

## Getting Started
```bash
# 인프라 실행
docker compose -f infra/docker-compose.yml up -d

# 백엔드 빌드
./gradlew build

# 프론트엔드
cd frontend && npm install && npm run dev
```

## Architecture
6 Microservices + API Gateway + Next.js Frontend
- See `CLAUDE.md` for full project guide
- See `.claude/TICKETS.md` for work plan
