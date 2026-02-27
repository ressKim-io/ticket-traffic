---
date: 2026-02-27
category: troubleshoot
project: sportstix
tags: [k3d, k8s, kafka, postgresql, redis, iptables, bitnami, probe]
---

# k3d 로컬 배포 시 4가지 이슈 해결 (포트 충돌, Bitnami 유료화, Kafka probe, iptables)

## Context
Docker Compose 기반 개발 환경을 k3d(K3s in Docker) 클러스터로 전환하는 작업.
로컬 PostgreSQL/Redis는 systemd로 실행하고, Kafka와 애플리케이션 서비스는 K8s Pod로 배포.
Observability 스택(OTel, Tempo, Loki, Grafana)도 K8s 내부에 배포.

---

## Issue 1: k3d 클러스터 생성 실패 — 포트 80 충돌

### 증상
```
ERRO Failed Cluster Start: Failed to add one or more helper nodes:
  runtime failed to start node 'k3d-sportstix-serverlb':
  failed to bind host port 0.0.0.0:80/tcp: address already in use
FATA Cluster creation FAILED, all changes have been rolled back!
```

### 재현 조건
`k3d cluster create sportstix --port "80:80@loadbalancer"` 실행 시 호스트의 포트 80이 이미 점유된 상태.

### Action
1. `ss -tlnp | grep ":80 "` 로 포트 점유 확인 → LISTEN 상태 확인, 프로세스 식별 불가 (sudo 필요)
2. 로컬에 nginx/apache 등 웹서버가 실행 중인 것으로 추정
3. 포트 충돌을 회피하여 대체 포트로 변경

### 수정
```bash
k3d cluster create sportstix \
  --port "8880:80@loadbalancer" \
  --port "8443:443@loadbalancer" \
  # ... 나머지 옵션 동일
```

### Result
k3d 클러스터 정상 생성 (1 server + 2 agents). Ingress 접근 시 `localhost:8880` 사용.

---

## Issue 2: Bitnami Kafka Helm 차트 이미지 pull 실패

### 증상
```
kubectl describe pod kafka-controller-0:
  Failed to pull image "docker.io/bitnami/kafka:4.0.0-debian-12-r10":
  docker.io/bitnami/kafka:4.0.0-debian-12-r10: not found
```

Helm 설치 시 WARNING 메시지:
```
Since August 28th, 2025, only a limited subset of images/charts are available for free.
Subscribe to Bitnami Secure Images to receive continued support.
```

### 재현 조건
`helm install kafka bitnami/kafka` 실행 시 Bitnami 유료 전환 후 이미지 접근 불가.

### Action
1. Bitnami Kafka 차트 v32.4.3이 kafka:4.0.0 이미지 사용 → Docker Hub에서 삭제됨
2. 대안: Docker Compose에서 사용하던 `apache/kafka:3.9.0` 공식 이미지로 직접 StatefulSet 작성

### 수정
- `helm uninstall kafka -n sportstix`
- `infra/k8s/local-dev/kafka.yaml` 신규 생성: Apache 공식 Kafka 3.9.0 이미지 + KRaft 모드 StatefulSet

### Result
Kafka Pod 정상 기동. `kafka.sportstix.svc.cluster.local:9092`로 서비스 접근 가능.

### 재발 방지
Bitnami Helm 차트는 유료화 리스크가 있으므로, 로컬 개발에서는 공식 이미지 기반 매니페스트를 직접 관리.

---

## Issue 3: Kafka Pod CrashLoopBackOff — liveness probe 과부하

### 증상
```
kubectl describe pod kafka-0:
  Last State: Terminated, Reason: Error, Exit Code: 143
  Restart Count: 7+
```

Exit Code 143 = SIGTERM (liveness probe 실패로 kubelet이 강제 종료).

### 재현 조건
Kafka liveness/readiness probe로 `kafka-topics.sh --bootstrap-server localhost:9092 --list` 사용 시,
이 명령은 매번 새 JVM을 띄워 실행하므로 메모리/CPU 소비가 과도하고, timeout 초과로 probe 실패.

### Action
1. Kafka 로그 확인 → 정상 기동 후 shutdown 반복 (probe SIGTERM)
2. `kafka-topics.sh`는 JVM 기반 CLI → probe로 사용하기에 너무 무거움
3. TCP 소켓 probe로 변경 (포트 9092 연결 확인만)

### 수정
```yaml
# Before (exec probe - heavy JVM)
readinessProbe:
  exec:
    command: ["/bin/sh", "-c", "/opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --list"]

# After (TCP socket probe - lightweight)
startupProbe:
  tcpSocket:
    port: 9092
  failureThreshold: 30
  periodSeconds: 5
readinessProbe:
  tcpSocket:
    port: 9092
livenessProbe:
  tcpSocket:
    port: 9092
  initialDelaySeconds: 15
  periodSeconds: 20
```

### Result
Kafka Pod 안정적으로 1/1 Running 유지. Restart 0.

### 재발 방지
Kafka probe는 항상 TCP 소켓 또는 HTTP 엔드포인트 사용. JVM CLI(kafka-topics.sh 등)는 probe에 사용 금지.

---

## Issue 4: Spring Boot 서비스 전체 CrashLoop — PostgreSQL/Redis 연결 타임아웃

### 증상
```json
{"level":"ERROR","message":"Application run failed",
 "stack_trace":"...Unable to obtain connection from database: The connection attempt failed...
   Caused by: java.net.SocketTimeoutException: Connect timed out"}
```

모든 Java 서비스(auth, game, booking, payment, admin)에서 동일 증상.

### 재현 조건
k3d Pod(IP: 10.42.x.x)에서 `host.k3d.internal`(172.21.0.1)의 PostgreSQL 5432, Redis 6379에 접근 시 타임아웃.

### Action
1. PostgreSQL `listen_addresses = '*'` 확인 → 정상 (0.0.0.0:5432 LISTEN)
2. Redis bind 0.0.0.0 확인 → 정상 (0.0.0.0:6379 LISTEN)
3. pg_hba.conf에 172.0.0.0/8, 10.0.0.0/8 대역 추가 → 완료
4. busybox Pod에서 `nc -zv host.k3d.internal 5432` 테스트 → **Connection timed out**
5. 가설: iptables/nftables가 Docker 네트워크(172.21.0.0/16)에서 호스트 서비스로의 트래픽을 차단
6. `iptables -I INPUT -s 172.21.0.0/16 -j ACCEPT` 등 4개 규칙 추가 → **연결 성공**

### 근본 원인 (Root Cause)
Linux 호스트의 기본 iptables INPUT/FORWARD 체인이 Docker bridge 네트워크(172.21.0.0/16) 및
k3d Pod 네트워크(10.42.0.0/16)에서 오는 트래픽을 DROP 처리.
PostgreSQL/Redis는 0.0.0.0에서 리스닝하지만, 방화벽 레벨에서 차단됨.

### 수정
```bash
sudo iptables -I INPUT -s 172.21.0.0/16 -j ACCEPT
sudo iptables -I INPUT -s 10.42.0.0/16 -j ACCEPT
sudo iptables -I FORWARD -s 172.21.0.0/16 -j ACCEPT
sudo iptables -I FORWARD -s 10.42.0.0/16 -j ACCEPT
```

### Result
busybox 테스트 Pod에서 PostgreSQL/Redis 모두 연결 성공.
전체 7개 서비스 + Kafka Pod 재시작 후 모두 1/1 Running, health=UP 확인.

### 재발 방지
- k3d 클러스터 생성 후 iptables 허용 규칙 추가를 표준 절차에 포함
- iptables 규칙은 재부팅 시 초기화되므로, 필요 시 `iptables-persistent` 패키지 또는 스크립트 자동화
- 향후 k3d 사용 시 `--network host` 옵션 또는 `docker network connect` 검토

---

## Related Files
- `infra/k8s/local-dev/kafka.yaml` — Kafka StatefulSet (신규, probe 수정)
- `infra/k8s/local-dev/external-services.yaml` — ExternalName Service for host.k3d.internal
- `infra/k8s/namespace.yaml` — Istio injection 레이블 제거
- `infra/k8s/services/*.yaml` — Istio sidecar 어노테이션 제거, replicas 축소
- `infra/k8s/services/booking.yaml` — Istio trafficRouting 제거, 기본 canary로 변경
- `infra/k8s/services/kustomization.yaml` — image tag latest → dev
- `infra/k8s/secrets/*.yaml` — 로컬 개발용 비밀번호/RSA 키 설정
- `infra/k8s/configmaps/*.yaml` — postgres-external, redis-external 호스트 변경
- `/etc/postgresql/17/main/pg_hba.conf` — k3d 대역 접근 허용
- `/etc/postgresql/17/main/postgresql.conf` — listen_addresses = '*'
- `/etc/redis/redis.conf` — bind 0.0.0.0, protected-mode no
