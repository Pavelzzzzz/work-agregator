#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
NAMESPACE="vacancy-scout"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

info()  { echo -e "${GREEN}[INFO]${NC} $*"; }
warn()  { echo -e "${YELLOW}[WARN]${NC} $*"; }
error() { echo -e "${RED}[ERROR]${NC} $*"; exit 1; }

# Check prerequisites
command -v kubectl >/dev/null 2>&1 || error "kubectl not found. Install it first."
command -v docker >/dev/null 2>&1 || error "docker not found."

# Step 1: Build images locally (for k3s docker runtime)
info "Building backend image..."
docker build -t vacancy-backend:latest -f backend/Dockerfile .

info "Building frontend image..."
docker build -t vacancy-frontend:latest -f frontend/Dockerfile .

# Step 2: Deploy to k3s
info "Applying manifests..."
kubectl apply -f "$SCRIPT_DIR/00-namespace.yaml"
kubectl apply -f "$SCRIPT_DIR/01-secrets.yaml"
kubectl apply -f "$SCRIPT_DIR/02-database.yaml"
kubectl apply -f "$SCRIPT_DIR/03-redis.yaml"

info "Waiting for database to be ready..."
kubectl wait --for=condition=ready pod/vacancy-scout-db-0 \
  -n "$NAMESPACE" --timeout=120s || warn "DB pod not ready yet, continuing..."

kubectl apply -f "$SCRIPT_DIR/04-backend.yaml"
kubectl apply -f "$SCRIPT_DIR/05-frontend.yaml"
kubectl apply -f "$SCRIPT_DIR/06-ingress.yaml"

# Step 3: Wait for pods
info "Waiting for pods to be ready..."
kubectl rollout status deployment/vacancy-scout-backend -n "$NAMESPACE" --timeout=120s
kubectl rollout status deployment/vacancy-scout-frontend -n "$NAMESPACE" --timeout=60s

info "Deployment complete!"
echo ""
info "Services:"
kubectl get svc -n "$NAMESPACE"
echo ""
info "Pods:"
kubectl get pods -n "$NAMESPACE"
echo ""
info "To access the app:"
echo "  Add to /etc/hosts: 127.0.0.1 vacancy.local"
echo "  Open: http://vacancy.local"
