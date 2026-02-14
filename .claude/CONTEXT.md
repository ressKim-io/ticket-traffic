# Project Context (for Claude Code reference)

This file supplements CLAUDE.md with project history, architectural decisions, technical pitfalls, and remaining backlog. CLAUDE.md has project rules and conventions; this file has accumulated knowledge.

## PR History

### Phase 1: Core Application (PR #1~35)
- PR #1: Mono-repo skeleton + Docker Compose infra
- PR #2: Common module (ApiResponse, ErrorCode, BaseEntity, Events)
- PR #3: API Gateway (Spring Cloud Gateway, WebFlux)
- PR #4: Auth Service (JWT, Spring Security)
- PR #5~6: Game Service (Stadium/Section/Seat, GameSeat mapping)
- PR #7~8: Queue Service (Redis Sorted Set, WebSocket STOMP)
- PR #9: WebSocket cross-pod scaling (Redis Pub/Sub)
- PR #10: Data Locality (local replica tables via Kafka)
- PR #11: jOOQ Hybrid Strategy (Hot Path SQL)
- PR #12: Payment Service (Mock PG)
- PR #13: Booking 3-tier Lock (Redis → DB Pessimistic → Optimistic)
- PR #14: Booking SAGA (orchestration with compensation)
- PR #15: Kafka Pipeline (DLQ, retry, idempotency)
- PR #16: Bot Prevention (UA fingerprint, rate limit, CAPTCHA)
- PR #17~23: Frontend (Next.js 14, Auth/Game/Queue/Seat/Payment/My pages)
- PR #24~32: Admin, Monitoring, E2E, K8s, Terraform, CI/CD, ArgoCD, K6
- PR #33: Data Reconciliation Batch
- PR #34: Test Coverage
- PR #35: API Docs + README

### Phase 1.5: Quality & Infra (PR #36~48)
- PR #36~37: CI fixes, quality cleanup
- PR #38: K8s security hardening (securityContext, RBAC)
- PR #39: OTel + structured logging (logstash-logback-encoder)
- PR #40: K8s observability stack (Tempo, Loki, Grafana)
- PR #41: Istio service mesh (east-west mTLS, AuthorizationPolicy)
- PR #42~43: Observability runtime fixes, CI kubeconform CRD skips
- PR #44: K8s security hardening HA (PDB, topology, revision limit)
- PR #45: Supply chain security (Trivy + SBOM + Cosign)
- PR #46: Policy-as-Code (Kyverno 7 ClusterPolicies)
- PR #47: External Secrets Operator (AWS Secrets Manager)
- PR #48: SLO/SLI + error budget (recording/alerting rules, Grafana)

### Phase 2: Resilience & Autoscaling (PR #49~54)
- PR #49: Argo Rollouts canary (booking, Istio VirtualService weight)
- PR #50: KEDA event-driven autoscaling (Kafka lag, Redis cardinality)
- PR #51: Velero backup & DR (S3 + EBS snapshots)
- PR #52~53: Chaos Mesh (PodChaos, NetworkChaos, Workflow, quality fixes)
- PR #54: CI kubeconform kustomization skip + DevOps README

### Phase 3: Platform Engineering (PR #55~62)
- PR #55: ArgoCD ApplicationSet (multi-env dev/staging/prod)
- PR #56: ArgoCD Notifications (Slack deployment alerts)
- PR #57: cert-manager (Let's Encrypt TLS automation)
- PR #58: Falco (runtime security, custom sportstix rules)
- PR #59: OpenCost (FinOps cost allocation)
- PR #60: Goldilocks (VPA right-sizing recommendations)
- PR #61: Backstage IDP (service catalog + Golden Path template)
- PR #62: README Phase 3 update

### Phase 4: Istio Gateway Migration (PR #63~67)
- PR #63: Auth RS256 JWT + JWKS endpoint (HS256 → RS256 전환)
- PR #64: Istio IngressGateway migration (Gateway Service 제거)
- PR #65: Istio HA resilience (PDB, circuit breaker, retry, graceful degradation)
- PR #66: JWT review fixes (JWKS cache, PEM validation, JwksController tests)
- PR #67: README Phase 4 update

## Architecture Decisions

### Gateway Service → Istio IngressGateway (PR #63~64)
- **Before**: `Client → NGINX Ingress → Gateway Service(Java) → Istio Sidecar → Backend`
- **After**: `Client → Istio IngressGateway(Envoy) → Istio Sidecar → Backend`
- Gateway Service code remains in repo for Docker Compose local dev, but NOT deployed to K8s
- JWT changed from HS256 (symmetric) → RS256 (asymmetric) because Istio RequestAuthentication only supports RS256/ES256
- JWKS endpoint at `auth:8081/.well-known/jwks.json` with Cache-Control: 1h

### Data Strategy
- **Cold Path**: JPA + QueryDSL (CRUD operations)
- **Hot Path**: jOOQ (seat locking, bulk ops, local replica queries)
- Services NEVER call each other via REST for writes → Kafka only
- Local replica tables synced via Kafka events (Data Locality pattern)

### Locking Strategy (Booking)
1. Redis distributed lock (`lock:seat:{gameSeatId}`, TTL 5s)
2. DB pessimistic lock (`SELECT FOR UPDATE`)
3. Optimistic concurrency (`@Version`)

### WebSocket
- STOMP over WebSocket for queue position + seat status
- Cross-pod broadcasting via Redis Pub/Sub (`ws:broadcast:{channel}`)
- Istio: `h2UpgradePolicy: DO_NOT_UPGRADE` + VirtualService `timeout: 0s`

## Technical Pitfalls (Learned the Hard Way)

### Java/Spring
- **Self-invocation @Transactional bypass**: Need separate bean for DB transaction inside Redis lock callback
- **LazyInitializationException**: Pass entity ID (not entity) when crossing transaction boundaries (e.g., scheduler)
- **Gateway WebFlux constraint**: Cannot depend on common module (servlet-based). PEM parsing duplicated with comment.
- **RSA PEM format**: Only PKCS#8 (`BEGIN PRIVATE KEY`) supported. PKCS#1 (`BEGIN RSA PRIVATE KEY`) needs conversion via `openssl pkcs8 -topk8`
- **JWKS BigInteger sign bit**: `BigInteger.toByteArray()` adds leading zero byte. Must strip before base64url encoding.

### Istio
- **AuthorizationPolicy selector**: Only `matchLabels` supported, NOT `matchExpressions`
- **RequestAuthentication**: RS256/ES256 only (HS256 not supported) → JWKS endpoint required
- **EnvoyFilter Lua response body**: Use `setBytes()` to replace body; check content-type to preserve existing JSON from backends
- **VirtualService retry on WebSocket**: Don't add retry policy to WebSocket routes (persistent connections)

### Observability
- **Promtail on macOS**: Use Docker socket discovery (`docker_sd_configs`), NOT file path `/var/lib/docker/containers`
- **OTel Collector memory_limiter**: Must be less than container memory limit (e.g., 250MiB for 384Mi container)
- **OTel Java Agent**: Use `OTEL_METRICS_EXPORTER=none`, `OTEL_LOGS_EXPORTER=none` to avoid duplicate metrics/logs

### Infra
- **Kafka**: Use `apache/kafka:3.9.0` (NOT bitnami) for KRaft mode
- **Spring Cloud BOM**: `2023.0.3` for gateway (Boot 3.3.x compatibility)

## Istio Configuration Summary

| Resource | File | Purpose |
|----------|------|---------|
| IstioOperator | `istio/istio-operator.yaml` | IngressGW (HA, HPA 2-6), Istiod (priority, PDB) |
| Gateway | `istio/gateway.yaml` | TLS termination, HTTP→HTTPS redirect |
| RequestAuthentication | `istio/request-authentication.yaml` | JWT RS256 via JWKS |
| AuthorizationPolicy | `istio/jwt-authz-policy.yaml` | Public/protected path enforcement |
| EnvoyFilter (JWT) | `istio/envoy-filter-jwt-headers.yaml` | Lua: X-User-Id/X-User-Role injection |
| EnvoyFilter (Rate) | `istio/envoy-filter-rate-limit.yaml` | Local rate limit 50 RPS |
| EnvoyFilter (Bot) | `istio/envoy-filter-bot-detection.yaml` | Lua: UA pattern blocking |
| EnvoyFilter (Fallback) | `istio/envoy-filter-fallback.yaml` | Lua: JSON error responses (503/429/502/504) |
| VirtualService | `istio/north-south-virtualservice.yaml` | API routing + CORS + retry |
| VirtualService | `istio/frontend-virtualservice.yaml` | Frontend routing |
| DestinationRule | `istio/destination-rules.yaml` | Circuit breaker + connection pool per service |
| PeerAuthentication | `istio/peer-authentication.yaml` | STRICT mTLS |
| Sidecar | `istio/sidecar.yaml` | Egress scope: `./*`, `istio-system/*`, `observability/*` |
| PDB | `istio/ingressgateway-pdb.yaml` | minAvailable: 1 |
| PDB | `istio/istiod-pdb.yaml` | minAvailable: 1 |

## K8s Improvements Backlog

### P0 (Production Readiness)
- [ ] `:latest` → immutable image tags (CI/CD ArgoCD image updater)
- [ ] DB superuser 공유 → per-service DB users
- [ ] Infra K8s manifests → AWS managed services (RDS/ElastiCache/MSK) + Terraform

### P1 Remaining
- [ ] Loki/Tempo/Grafana HA (S3 backend, Effort: L)

### P2 (Nice-to-have)
- [ ] ResourceQuota / LimitRange
- [ ] PriorityClass (critical/normal/low)
- [ ] Trace sampling 100% → 10%
- [ ] Ingress security headers
- [ ] Grafana Prometheus datasource

## kubeconform Skip List
IstioOperator, Kiali, ClusterPolicy, ClusterSecretStore, ExternalSecret, Rollout, AnalysisTemplate, AnalysisRun, ScaledObject, TriggerAuthentication, BackupStorageLocation, VolumeSnapshotLocation, Schedule, PodChaos, NetworkChaos, Workflow, StatusCheck, VerticalPodAutoscaler, Gateway, RequestAuthentication, AuthorizationPolicy, EnvoyFilter, VirtualService, DestinationRule, Sidecar, PeerAuthentication
