#!/bin/bash
# k3d Local Development Environment Setup
# Automates: k3d cluster creation, iptables rules, Kafka deploy,
# image build+import, manifest apply, observability stack.
#
# Usage:
#   ./scripts/k3d-local-setup.sh          # Full setup (first time)
#   ./scripts/k3d-local-setup.sh restart   # After reboot (iptables + restart pods)
#   ./scripts/k3d-local-setup.sh build     # Rebuild images only
#   ./scripts/k3d-local-setup.sh iptables  # Reapply iptables rules only

set -euo pipefail

CLUSTER_NAME="sportstix"
NAMESPACE="sportstix"
OBS_NAMESPACE="observability"
PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
IMAGE_TAG="dev"

# Service list for Docker build
SERVICES=(auth-service game-service queue-service booking-service payment-service admin-service gateway)
FRONTEND=frontend

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log()  { echo -e "${GREEN}[INFO]${NC} $*"; }
warn() { echo -e "${YELLOW}[WARN]${NC} $*"; }
err()  { echo -e "${RED}[ERROR]${NC} $*"; }

# ─────────────────────────────────────────────
# iptables: Allow k3d/pod network → host services
# ─────────────────────────────────────────────
apply_iptables() {
  log "Applying iptables rules for k3d → host access..."
  sudo iptables -C INPUT -s 172.21.0.0/16 -j ACCEPT 2>/dev/null || sudo iptables -I INPUT -s 172.21.0.0/16 -j ACCEPT
  sudo iptables -C INPUT -s 10.42.0.0/16 -j ACCEPT 2>/dev/null  || sudo iptables -I INPUT -s 10.42.0.0/16 -j ACCEPT
  sudo iptables -C FORWARD -s 172.21.0.0/16 -j ACCEPT 2>/dev/null || sudo iptables -I FORWARD -s 172.21.0.0/16 -j ACCEPT
  sudo iptables -C FORWARD -s 10.42.0.0/16 -j ACCEPT 2>/dev/null  || sudo iptables -I FORWARD -s 10.42.0.0/16 -j ACCEPT
  log "iptables rules applied."
}

# ─────────────────────────────────────────────
# Ensure local PostgreSQL and Redis are running
# ─────────────────────────────────────────────
check_local_services() {
  log "Checking local PostgreSQL and Redis..."
  if ! systemctl is-active --quiet postgresql; then
    warn "PostgreSQL is not running. Starting..."
    sudo systemctl start postgresql
  fi
  if ! systemctl is-active --quiet redis-server; then
    warn "Redis is not running. Starting..."
    sudo systemctl start redis-server
  fi
  log "Local services: PostgreSQL=$(systemctl is-active postgresql), Redis=$(systemctl is-active redis-server)"
}

# ─────────────────────────────────────────────
# Initialize PostgreSQL databases and users
# ─────────────────────────────────────────────
init_databases() {
  log "Initializing PostgreSQL databases and users..."
  export POSTGRES_USER=postgres
  export PGPASSWORD=postgres
  bash "$PROJECT_ROOT/infra/init-db.sh"
  unset PGPASSWORD
  log "Databases initialized."
}

# ─────────────────────────────────────────────
# Create k3d cluster
# ─────────────────────────────────────────────
create_cluster() {
  if k3d cluster list 2>/dev/null | grep -q "$CLUSTER_NAME"; then
    log "k3d cluster '$CLUSTER_NAME' already exists. Skipping creation."
    return 0
  fi

  log "Creating k3d cluster '$CLUSTER_NAME'..."
  k3d cluster create "$CLUSTER_NAME" \
    --servers 1 --agents 2 \
    --port "8880:80@loadbalancer" \
    --port "8443:443@loadbalancer" \
    --k3s-arg "--disable=traefik@server:0"

  log "k3d cluster created."
}

# ─────────────────────────────────────────────
# Install Argo Rollouts CRD
# ─────────────────────────────────────────────
install_argo_rollouts() {
  if kubectl get namespace argo-rollouts &>/dev/null; then
    log "Argo Rollouts already installed. Skipping."
    return 0
  fi

  log "Installing Argo Rollouts..."
  kubectl create namespace argo-rollouts
  kubectl apply -n argo-rollouts -f https://github.com/argoproj/argo-rollouts/releases/latest/download/install.yaml
  log "Argo Rollouts installed."
}

# ─────────────────────────────────────────────
# Build Docker images and import to k3d
# ─────────────────────────────────────────────
build_and_import_images() {
  log "Building and importing Docker images..."
  cd "$PROJECT_ROOT"

  # Java services (parallel build)
  for svc in "${SERVICES[@]}"; do
    log "Building $svc..."
    docker build --build-arg SERVICE_NAME="$svc" -t "sportstix/$svc:$IMAGE_TAG" . &
  done

  # Frontend
  log "Building $FRONTEND..."
  docker build -t "sportstix/$FRONTEND:$IMAGE_TAG" -f frontend/Dockerfile frontend/ &

  # Wait for all builds
  wait
  log "All images built."

  # Import to k3d
  log "Importing images to k3d cluster..."
  local images=()
  for svc in "${SERVICES[@]}"; do
    images+=("sportstix/$svc:$IMAGE_TAG")
  done
  images+=("sportstix/$FRONTEND:$IMAGE_TAG")

  k3d image import "${images[@]}" -c "$CLUSTER_NAME"
  log "Images imported to k3d."
}

# ─────────────────────────────────────────────
# Deploy K8s manifests
# ─────────────────────────────────────────────
deploy_manifests() {
  log "Deploying K8s manifests..."
  cd "$PROJECT_ROOT"

  # Namespace
  kubectl apply -f infra/k8s/namespace.yaml

  # RBAC
  kubectl apply -f infra/k8s/rbac/

  # ConfigMaps & Secrets
  kubectl apply -f infra/k8s/configmaps/
  kubectl apply -f infra/k8s/secrets/

  # Local dev (ExternalName services for PostgreSQL/Redis)
  kubectl apply -f infra/k8s/local-dev/

  # Application services (Kustomize)
  kubectl apply -k infra/k8s/services/

  log "Application manifests deployed."
}

# ─────────────────────────────────────────────
# Deploy Observability stack
# ─────────────────────────────────────────────
deploy_observability() {
  log "Deploying Observability stack..."
  cd "$PROJECT_ROOT"

  kubectl apply -f infra/k8s/observability/namespace.yaml
  kubectl apply -f infra/k8s/observability/rbac.yaml
  kubectl apply -f infra/k8s/observability/otel-collector.yaml
  kubectl apply -f infra/k8s/observability/tempo.yaml
  kubectl apply -f infra/k8s/observability/loki.yaml
  kubectl apply -f infra/k8s/observability/promtail.yaml
  kubectl apply -f infra/k8s/observability/grafana.yaml

  log "Observability stack deployed."
}

# ─────────────────────────────────────────────
# Wait and verify pods
# ─────────────────────────────────────────────
verify_pods() {
  log "Waiting for pods to be ready..."
  echo ""
  echo "=== $NAMESPACE namespace ==="
  kubectl get pods -n "$NAMESPACE"
  echo ""
  echo "=== $OBS_NAMESPACE namespace ==="
  kubectl get pods -n "$OBS_NAMESPACE"
  echo ""
  log "Use 'kubectl get pods -n $NAMESPACE -w' to watch pod status."
  log "Use 'kubectl port-forward -n sportstix svc/auth 8081:8081' to test health."
}

# ─────────────────────────────────────────────
# Restart pods (useful after reboot + iptables)
# ─────────────────────────────────────────────
restart_pods() {
  log "Restarting all pods in $NAMESPACE..."
  kubectl delete pods --all -n "$NAMESPACE"
  log "Restarting all pods in $OBS_NAMESPACE..."
  kubectl delete pods --all -n "$OBS_NAMESPACE"
  log "Pods restarting. Use 'kubectl get pods -n $NAMESPACE -w' to watch."
}

# ─────────────────────────────────────────────
# Main
# ─────────────────────────────────────────────
case "${1:-full}" in
  full)
    log "=== Full k3d Local Setup ==="
    check_local_services
    init_databases
    create_cluster
    apply_iptables
    install_argo_rollouts
    build_and_import_images
    deploy_observability
    deploy_manifests
    verify_pods
    log "=== Setup Complete ==="
    log "Ingress: http://localhost:8880"
    log "Grafana: kubectl port-forward -n observability svc/grafana 3001:3000"
    ;;
  restart)
    log "=== Post-Reboot Recovery ==="
    check_local_services
    apply_iptables
    restart_pods
    verify_pods
    log "=== Recovery Complete ==="
    ;;
  build)
    log "=== Rebuild Images ==="
    build_and_import_images
    log "=== Build Complete ==="
    ;;
  iptables)
    apply_iptables
    ;;
  *)
    echo "Usage: $0 {full|restart|build|iptables}"
    echo ""
    echo "  full     - Complete first-time setup (default)"
    echo "  restart  - After reboot: start services, iptables, restart pods"
    echo "  build    - Rebuild Docker images and import to k3d"
    echo "  iptables - Reapply iptables rules only"
    exit 1
    ;;
esac
