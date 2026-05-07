#!/usr/bin/env bash
# deploy.sh
#
# Build frontend and backend, upload artifacts to the remote server,
# and restart services. Caddy is assumed to be pre-configured on the server.
#
# Prerequisites (local):
#   - Java 21, Maven wrapper (backend/mvnw)
#   - Node.js 22 + npm (frontend)
#   - rsync, ssh
#
# Prerequisites (remote):
#   - Java 21 available
#   - systemd service for the backend (see REMOTE_SERVICE_NAME)
#   - Caddy serving frontend files from REMOTE_FRONTEND_DIR
#   - SSH key-based auth configured for REMOTE_USER@REMOTE_HOST
#
# Usage:
#   ./scripts/deploy.sh [--skip-tests] [--frontend-only] [--backend-only]

set -euo pipefail

# ─── Configuration ────────────────────────────────────────────────────────────
REMOTE_HOST="192.168.10.128"
REMOTE_USER="${DEPLOY_USER:-deploy}"
REMOTE_SSH_KEY="${DEPLOY_SSH_KEY:-}"          # e.g. ~/.ssh/id_ed25519_psy; empty = use default

REMOTE_FRONTEND_DIR="/var/www/html"
REMOTE_BACKEND_DIR="/opt/psy-assistant"
REMOTE_SERVICE_NAME="psy-assistant"

BACKEND_JAR_NAME="psy-assistant-backend-0.0.1-SNAPSHOT.jar"
SPRING_PROFILE="prod"

# ─── Flags ────────────────────────────────────────────────────────────────────
SKIP_TESTS=false
BUILD_FRONTEND=true
BUILD_BACKEND=true

for arg in "$@"; do
  case "$arg" in
    --skip-tests)    SKIP_TESTS=true ;;
    --frontend-only) BUILD_BACKEND=false ;;
    --backend-only)  BUILD_FRONTEND=false ;;
    *) echo "Unknown argument: $arg"; exit 1 ;;
  esac
done

# ─── Helpers ──────────────────────────────────────────────────────────────────
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

log()  { echo "[deploy] $*"; }
warn() { echo "[deploy] WARN: $*" >&2; }
die()  { echo "[deploy] ERROR: $*" >&2; exit 1; }

ssh_opts=(-o StrictHostKeyChecking=no -o BatchMode=yes)
[[ -n "$REMOTE_SSH_KEY" ]] && ssh_opts+=(-i "$REMOTE_SSH_KEY")

remote_exec() {
  ssh "${ssh_opts[@]}" "${REMOTE_USER}@${REMOTE_HOST}" "$@"
}

rsync_to_remote() {
  local src="$1"
  local dst="$2"
  local ssh_cmd="ssh"
  [[ -n "$REMOTE_SSH_KEY" ]] && ssh_cmd="ssh -i $REMOTE_SSH_KEY -o StrictHostKeyChecking=no"
  # --rsync-path="sudo rsync" runs the remote rsync as root so web-root ownership never blocks writes
  rsync -az --delete --progress \
    --rsync-path="sudo rsync" \
    -e "$ssh_cmd" \
    "$src" "${REMOTE_USER}@${REMOTE_HOST}:${dst}"
}

scp_to_remote() {
  local src="$1"
  local dst="$2"
  scp "${ssh_opts[@]}" "$src" "${REMOTE_USER}@${REMOTE_HOST}:${dst}"
}

# ─── Preflight ────────────────────────────────────────────────────────────────
log "Preflight checks..."
command -v rsync >/dev/null || die "rsync is not installed"
command -v ssh   >/dev/null || die "ssh is not installed"

if $BUILD_FRONTEND; then
  command -v node >/dev/null || die "node is not installed"
  command -v npm  >/dev/null || die "npm is not installed"
fi

if $BUILD_BACKEND; then
  [[ -f "$PROJECT_ROOT/backend/mvnw" ]] || die "mvnw not found in backend/"
fi

log "Connecting to ${REMOTE_USER}@${REMOTE_HOST}..."
remote_exec "echo 'SSH connection OK'" || die "Cannot connect to remote server"

# ─── Build Frontend ───────────────────────────────────────────────────────────
if $BUILD_FRONTEND; then
  log "Building frontend (production)..."
  cd "$PROJECT_ROOT/frontend"
  npm ci --silent

  # Stamp a build version so Transloco cache-busts i18n JSON files after each deploy
  BUILD_TS="$(date -u +%Y%m%d%H%M%S)"
  sed -i.bak "s/appVersion: '[^']*'/appVersion: '${BUILD_TS}'/" \
    src/app/environments/environment.prod.ts && rm -f src/app/environments/environment.prod.ts.bak
  log "Build version stamped: ${BUILD_TS}"

  npx ng build --configuration production
  FRONTEND_DIST="$PROJECT_ROOT/frontend/dist/frontend/browser"
  [[ -d "$FRONTEND_DIST" ]] || die "Frontend build output not found: $FRONTEND_DIST"
  log "Frontend build OK → $FRONTEND_DIST"
fi

# ─── Build Backend ────────────────────────────────────────────────────────────
if $BUILD_BACKEND; then
  log "Building backend..."
  cd "$PROJECT_ROOT/backend"

  if $SKIP_TESTS; then
    log "  (skipping tests)"
    ./mvnw package -DskipTests -q
  else
    ./mvnw verify -q
  fi

  BACKEND_JAR="$PROJECT_ROOT/backend/target/$BACKEND_JAR_NAME"
  [[ -f "$BACKEND_JAR" ]] || die "Backend JAR not found: $BACKEND_JAR"
  log "Backend build OK → $BACKEND_JAR"
fi

# ─── Upload Frontend ──────────────────────────────────────────────────────────
if $BUILD_FRONTEND; then
  log "Uploading frontend to ${REMOTE_HOST}:${REMOTE_FRONTEND_DIR}..."
  remote_exec "mkdir -p $REMOTE_FRONTEND_DIR"
  rsync_to_remote "$FRONTEND_DIST/" "$REMOTE_FRONTEND_DIR/"
  log "Frontend upload OK"
fi

# ─── Upload Backend ───────────────────────────────────────────────────────────
if $BUILD_BACKEND; then
  log "Uploading backend JAR to ${REMOTE_HOST}:${REMOTE_BACKEND_DIR}..."
  remote_exec "mkdir -p $REMOTE_BACKEND_DIR"
  scp_to_remote "$BACKEND_JAR" "$REMOTE_BACKEND_DIR/$BACKEND_JAR_NAME"
  log "Backend upload OK"
fi

# ─── Restart Backend Service ──────────────────────────────────────────────────
if $BUILD_BACKEND; then
  log "Restarting backend service '$REMOTE_SERVICE_NAME'..."
  remote_exec "sudo systemctl restart $REMOTE_SERVICE_NAME"

  log "Waiting for backend to become healthy..."
  for i in $(seq 1 30); do
    if remote_exec "curl -sf http://localhost:8080/actuator/health >/dev/null 2>&1"; then
      log "Backend is healthy (attempt $i)"
      break
    fi
    if [[ $i -eq 30 ]]; then
      warn "Backend health check timed out — check 'journalctl -u $REMOTE_SERVICE_NAME' on the server"
    fi
    sleep 2
  done
fi

# ─── Upload Caddyfile and reload ──────────────────────────────────────────────
CADDYFILE_SRC="$SCRIPT_DIR/Caddyfile"
if [[ -f "$CADDYFILE_SRC" ]]; then
  log "Uploading Caddyfile..."
  TMP_CADDY=$(remote_exec "mktemp /tmp/Caddyfile.XXXXXX")
  scp_to_remote "$CADDYFILE_SRC" "$TMP_CADDY"
  remote_exec "sudo bash -c 'mkdir -p /etc/caddy && mv $TMP_CADDY /etc/caddy/Caddyfile && chmod 644 /etc/caddy/Caddyfile'"
  log "Caddyfile uploaded"
fi

log "Reloading Caddy..."
remote_exec "sudo systemctl reload caddy || sudo caddy reload --config /etc/caddy/Caddyfile 2>/dev/null || true"

# ─── Done ─────────────────────────────────────────────────────────────────────
log "Deployment complete."
log ""
log "  Host      : https://${REMOTE_HOST}"
log "  Service   : ${REMOTE_SERVICE_NAME}"
log "  Frontend  : ${REMOTE_FRONTEND_DIR}"
log "  Backend   : ${REMOTE_BACKEND_DIR}/${BACKEND_JAR_NAME}"
