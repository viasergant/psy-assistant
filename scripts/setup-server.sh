#!/usr/bin/env bash
# setup-server.sh
#
# One-time server provisioning script.
# Runs remotely via SSH and sets up everything needed to run psy-assistant:
#   - Java 21 (Temurin via Eclipse Adoptium repository)
#   - rsync (required by deploy.sh)
#   - Application user and directories
#   - systemd service unit for the Spring Boot backend
#   - sudoers rules so the deploy user can manage the service and reload Caddy
#
# Usage:
#   ./scripts/setup-server.sh
#   DEPLOY_USER=ubuntu DEPLOY_SSH_KEY=~/.ssh/id_ed25519 ./scripts/setup-server.sh
#
# Run once before the first deploy. Safe to re-run — all steps are idempotent.

set -euo pipefail

# ─── Configuration ────────────────────────────────────────────────────────────
REMOTE_HOST="192.168.10.128"
REMOTE_USER="${DEPLOY_USER:-deploy}"          # SSH user (must have sudo access)
REMOTE_SSH_KEY="${DEPLOY_SSH_KEY:-}"

APP_USER="psyassistant"                       # Unprivileged user that runs the JVM
BACKEND_DIR="/opt/psy-assistant"
FRONTEND_DIR="/var/www/html"
JAR_NAME="psy-assistant-backend-0.0.1-SNAPSHOT.jar"
SERVICE_NAME="psy-assistant"
SPRING_PROFILE="prod"
APP_PORT="8080"

# ─── Helpers ──────────────────────────────────────────────────────────────────
log()  { echo "[setup] $*"; }
die()  { echo "[setup] ERROR: $*" >&2; exit 1; }

ssh_opts=(-o StrictHostKeyChecking=no)
[[ -n "$REMOTE_SSH_KEY" ]] && ssh_opts+=(-i "$REMOTE_SSH_KEY")

remote_exec() {
  ssh "${ssh_opts[@]}" "${REMOTE_USER}@${REMOTE_HOST}" "$@"
}

remote_sudo() {
  # Run a block of commands as root on the remote host.
  # Strategy: write the script to a temp file (stdin free for that),
  # then run it in a second ssh -t call so sudo can prompt for a password.
  local tmpfile
  tmpfile=$(remote_exec "mktemp /tmp/setup-XXXXXX.sh")
  # Upload the script content (no TTY needed here)
  ssh "${ssh_opts[@]}" "${REMOTE_USER}@${REMOTE_HOST}" "cat > $tmpfile && chmod 700 $tmpfile" <<EOF
$*
EOF
  # Execute with a real TTY so sudo can ask for a password
  ssh -t "${ssh_opts[@]}" "${REMOTE_USER}@${REMOTE_HOST}" "sudo bash $tmpfile; sudo rm -f $tmpfile"
}

# ─── Preflight ────────────────────────────────────────────────────────────────
log "Preflight checks..."
command -v ssh >/dev/null || die "ssh is not installed locally"

log "Connecting to ${REMOTE_USER}@${REMOTE_HOST}..."
remote_exec "echo 'SSH OK'" || die "Cannot connect — check REMOTE_USER / REMOTE_SSH_KEY"

# ─── Detect OS ────────────────────────────────────────────────────────────────
log "Detecting OS..."
DISTRO=$(remote_exec "cat /etc/os-release | grep ^ID= | cut -d= -f2 | tr -d '\"'")
log "  Distro: $DISTRO"

case "$DISTRO" in
  ubuntu|debian) PKG="apt-get" ;;
  centos|rhel|fedora|almalinux|rocky) PKG="dnf" ;;
  *) die "Unsupported distro: $DISTRO. Add it to the case statement." ;;
esac

# ─── System packages ──────────────────────────────────────────────────────────
log "Installing system packages (rsync, curl, ca-certificates)..."
if [[ "$PKG" == "apt-get" ]]; then
  remote_sudo "
    export DEBIAN_FRONTEND=noninteractive
    apt-get update -qq
    apt-get install -y -qq rsync curl ca-certificates gnupg apt-transport-https
  "
else
  remote_sudo "
    dnf install -y rsync curl ca-certificates
  "
fi

# ─── Java 21 (Eclipse Temurin) ────────────────────────────────────────────────
log "Installing Java 21 (Eclipse Temurin)..."
if remote_exec "java -version 2>&1 | grep -q '21'"; then
  log "  Java 21 already installed, skipping"
else
  if [[ "$PKG" == "apt-get" ]]; then
    remote_sudo "
      # Add Adoptium GPG key and repo
      mkdir -p /etc/apt/keyrings
      curl -fsSL https://packages.adoptium.net/artifactory/api/gpg/key/public \
        | gpg --dearmor -o /etc/apt/keyrings/adoptium.gpg
      echo \"deb [signed-by=/etc/apt/keyrings/adoptium.gpg] https://packages.adoptium.net/artifactory/deb \$(. /etc/os-release; echo \$VERSION_CODENAME) main\" \
        > /etc/apt/sources.list.d/adoptium.list
      apt-get update -qq
      apt-get install -y -qq temurin-21-jdk
    "
  else
    remote_sudo "
      # RHEL-based: use Adoptium RPM repo
      cat > /etc/yum.repos.d/adoptium.repo <<'REPO'
[Adoptium]
name=Adoptium
baseurl=https://packages.adoptium.net/artifactory/rpm/rhel/\$releasever/\$basearch
enabled=1
gpgcheck=1
gpgkey=https://packages.adoptium.net/artifactory/api/gpg/key/public
REPO
      dnf install -y temurin-21-jdk
    "
  fi
  log "  Java 21 installed"
fi

# ─── Application user ─────────────────────────────────────────────────────────
log "Creating application user '$APP_USER'..."
remote_sudo "
  if ! id $APP_USER &>/dev/null; then
    useradd --system --no-create-home --shell /usr/sbin/nologin $APP_USER
    echo '  User created'
  else
    echo '  User already exists'
  fi
"

# ─── Directories ──────────────────────────────────────────────────────────────
log "Creating directories..."
remote_sudo "
  mkdir -p $BACKEND_DIR $FRONTEND_DIR /var/log/psy-assistant
  chown $APP_USER:$APP_USER $BACKEND_DIR /var/log/psy-assistant
  chmod 750 $BACKEND_DIR /var/log/psy-assistant
  chown -R www-data:www-data $FRONTEND_DIR 2>/dev/null || chown -R caddy:caddy $FRONTEND_DIR 2>/dev/null || true
  chmod 755 $FRONTEND_DIR
"

# ─── env file (secrets placeholder) ──────────────────────────────────────────
ENV_FILE="$BACKEND_DIR/psy-assistant.env"
log "Creating env file $ENV_FILE (if not present)..."
remote_sudo "
  if [[ ! -f $ENV_FILE ]]; then
    cat > $ENV_FILE <<'ENVFILE'
# psy-assistant runtime environment
# Fill in real values — this file is NOT committed to git.
SPRING_PROFILES_ACTIVE=$SPRING_PROFILE
DB_URL=jdbc:postgresql://localhost:5432/psyassistant
DB_USERNAME=psyassistant
DB_PASSWORD=CHANGE_ME
JWT_SECRET=CHANGE_ME
SESSION_NOTES_ENCRYPTION_KEY=CHANGE_ME
ENVFILE
    chmod 640 $ENV_FILE
    chown root:$APP_USER $ENV_FILE
    echo '  Created — fill in real values before starting the service'
  else
    echo '  Already exists, not overwriting'
  fi
"

# ─── systemd service unit ─────────────────────────────────────────────────────
log "Installing systemd service '$SERVICE_NAME'..."
JAVA_BIN=$(remote_exec "which java || echo /usr/bin/java")
remote_sudo "
  cat > /etc/systemd/system/${SERVICE_NAME}.service <<UNIT
[Unit]
Description=Psy-Assistant Spring Boot Backend
Documentation=https://github.com/your-org/psy-assistant
After=network.target postgresql.service
Wants=postgresql.service

[Service]
Type=simple
User=$APP_USER
Group=$APP_USER
WorkingDirectory=$BACKEND_DIR
EnvironmentFile=$ENV_FILE
ExecStart=$JAVA_BIN -jar $BACKEND_DIR/$JAR_NAME --server.port=$APP_PORT
ExecStop=/bin/kill -s TERM \$MAINPID
SuccessExitStatus=143
Restart=on-failure
RestartSec=10
StandardOutput=append:/var/log/psy-assistant/app.log
StandardError=append:/var/log/psy-assistant/app.log
SyslogIdentifier=$SERVICE_NAME

# Hardening
NoNewPrivileges=true
PrivateTmp=true
ProtectSystem=strict
ReadWritePaths=$BACKEND_DIR /var/log/psy-assistant

[Install]
WantedBy=multi-user.target
UNIT

  systemctl daemon-reload
  systemctl enable $SERVICE_NAME
  echo '  Service unit installed and enabled'
"

# ─── sudoers rules for deploy user ───────────────────────────────────────────
log "Configuring sudoers for deploy user '$REMOTE_USER'..."
remote_sudo "
  SUDOERS_FILE=/etc/sudoers.d/psy-assistant-deploy
  cat > \$SUDOERS_FILE <<SUDOERS
# Allow the deploy user to manage psy-assistant service and reload Caddy
$REMOTE_USER ALL=(root) NOPASSWD: /usr/bin/systemctl restart $SERVICE_NAME
$REMOTE_USER ALL=(root) NOPASSWD: /usr/bin/systemctl start $SERVICE_NAME
$REMOTE_USER ALL=(root) NOPASSWD: /usr/bin/systemctl stop $SERVICE_NAME
$REMOTE_USER ALL=(root) NOPASSWD: /usr/bin/systemctl status $SERVICE_NAME
$REMOTE_USER ALL=(root) NOPASSWD: /usr/bin/systemctl reload caddy
$REMOTE_USER ALL=(root) NOPASSWD: /usr/bin/caddy reload --config /etc/caddy/Caddyfile
$REMOTE_USER ALL=(root) NOPASSWD: /usr/bin/mkdir -p $BACKEND_DIR
$REMOTE_USER ALL=(root) NOPASSWD: /usr/bin/mkdir -p $FRONTEND_DIR
$REMOTE_USER ALL=(root) NOPASSWD: /usr/bin/rsync
SUDOERS
  chmod 440 \$SUDOERS_FILE
  visudo -cf \$SUDOERS_FILE && echo '  sudoers OK' || (rm \$SUDOERS_FILE; echo 'ERROR: sudoers file invalid, removed')
"

# ─── Write permissions for deploy user ───────────────────────────────────────
log "Granting deploy user write access to app directories..."
remote_sudo "
  # Add deploy user to app group so they can scp the JAR into place
  usermod -aG $APP_USER $REMOTE_USER 2>/dev/null || true
  chown $APP_USER:$APP_USER $BACKEND_DIR
  chmod 775 $BACKEND_DIR
"

# ─── Caddy ────────────────────────────────────────────────────────────────────
log "Installing Caddy..."
if ! remote_exec "command -v caddy >/dev/null 2>&1"; then
  if [[ "$PKG" == "apt-get" ]]; then
    remote_sudo "
      apt-get install -y -qq debian-keyring debian-archive-keyring apt-transport-https
      curl -1sLf 'https://dl.cloudsmith.io/public/caddy/stable/gpg.key' \
        | gpg --dearmor -o /usr/share/keyrings/caddy-stable-archive-keyring.gpg
      curl -1sLf 'https://dl.cloudsmith.io/public/caddy/stable/debian.deb.txt' \
        | tee /etc/apt/sources.list.d/caddy-stable.list
      apt-get update -qq
      apt-get install -y -qq caddy
    "
  else
    remote_sudo "
      dnf install -y 'dnf-command(copr)'
      dnf copr enable -y @caddy/caddy
      dnf install -y caddy
    "
  fi
  log "  Caddy installed"
else
  log "  Caddy already installed, skipping"
fi

# ─── Caddyfile ────────────────────────────────────────────────────────────────
log "Deploying Caddyfile..."
CADDYFILE_SRC="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/Caddyfile"
[[ -f "$CADDYFILE_SRC" ]] || die "Caddyfile not found at $CADDYFILE_SRC"

# Upload to a temp location then move to /etc/caddy/Caddyfile as root
TMP_CADDY=$(remote_exec "mktemp /tmp/Caddyfile.XXXXXX")
scp "${ssh_opts[@]}" "$CADDYFILE_SRC" "${REMOTE_USER}@${REMOTE_HOST}:${TMP_CADDY}"
remote_sudo "
  mkdir -p /etc/caddy
  mv $TMP_CADDY /etc/caddy/Caddyfile
  chown root:caddy /etc/caddy/Caddyfile 2>/dev/null || chown root:root /etc/caddy/Caddyfile
  chmod 644 /etc/caddy/Caddyfile
  caddy validate --config /etc/caddy/Caddyfile
  systemctl enable caddy
  systemctl restart caddy
"
log "  Caddyfile deployed and Caddy restarted"

# ─── Done ─────────────────────────────────────────────────────────────────────
log ""
log "Server setup complete."
log ""
log "  NEXT STEPS:"
log "  1. Edit the env file on the server and fill in real secrets:"
log "       ssh ${REMOTE_USER}@${REMOTE_HOST} 'sudo nano $ENV_FILE'"
log "  2. Run the deploy script:"
log "       ./scripts/deploy.sh --skip-tests"
log "  3. Check service status:"
log "       ssh ${REMOTE_USER}@${REMOTE_HOST} 'sudo systemctl status $SERVICE_NAME'"
log "  4. Tail logs:"
log "       ssh ${REMOTE_USER}@${REMOTE_HOST} 'tail -f /var/log/psy-assistant/app.log'"
