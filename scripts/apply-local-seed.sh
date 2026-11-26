#!/usr/bin/env bash
# apply-local-seed.sh
#
# Applies a local-seed SQL file to the LOCAL development database without starting the backend.
# Default file: the personal therapist seed (alphanet.vin@gmail.com / 111, linked profile).
#
# Usage (from anywhere):
#   ./scripts/apply-local-seed.sh                       # apply the personal therapist seed
#   ./scripts/apply-local-seed.sh path/to/other.sql     # apply another seed file
#   ./scripts/apply-local-seed.sh --dry-run             # show what would run, change nothing
#
# Connection (defaults match application-local.yml; override with env vars):
#   DB_HOST=192.168.10.146  DB_PORT=5432  DB_NAME=psyassistant_local
#   DB_USER=psyassistant    DB_PASSWORD=psyassistant
#
# How psql is reached (first match wins):
#   --ssh / DB_SSH=user@host   run psql ON the DB host over SSH (default target: $DB_SSH_USER@$DB_HOST,
#                              DB_SSH_USER defaults to your login name). Needs psql on that host and
#                              key-based SSH. The DB password is sent over stdin, never on a command line.
#   local psql                 connect from this machine to DB_HOST:DB_PORT
#   docker                     `docker run postgres:16 psql` (daemon must be running)
# Without --ssh the order is: local psql, then SSH to the DB host, then docker.
# Refuses to touch a database that does not look local (name ending in _local, or host localhost)
# unless ALLOW_NON_LOCAL=1 is set. The seed files are idempotent, so re-running is safe.

set -euo pipefail

RED='\033[0;31m'; GREEN='\033[0;32m'; BOLD='\033[1m'; RESET='\033[0m'
info()    { echo -e "${BOLD}[INFO]${RESET}  $*"; }
success() { echo -e "${GREEN}[OK]${RESET}    $*"; }
fail()    { echo -e "${RED}[ERROR]${RESET} $*" >&2; exit 1; }

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
DEFAULT_SEED="$PROJECT_ROOT/backend/src/main/resources/db/local-seed/R__seed_personal_therapist.sql"

DB_HOST="${DB_HOST:-192.168.10.146}"
DB_PORT="${DB_PORT:-5432}"
DB_NAME="${DB_NAME:-psyassistant_local}"
DB_USER="${DB_USER:-psyassistant}"
DB_PASSWORD="${DB_PASSWORD:-psyassistant}"

DB_SSH="${DB_SSH:-}"
FORCE_SSH=0
DRY_RUN=0
SEED_FILE="$DEFAULT_SEED"
for arg in "$@"; do
    case "$arg" in
        --dry-run) DRY_RUN=1 ;;
        --ssh)     FORCE_SSH=1 ;;
        -h|--help) sed -n '2,28p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'; exit 0 ;;
        *)         SEED_FILE="$arg" ;;
    esac
done

[ -f "$SEED_FILE" ] || fail "Seed file not found: $SEED_FILE"

# ─── Safety: only local-looking databases ─────────────────────────────────────
is_local=0
case "$DB_NAME" in *_local) is_local=1 ;; esac
case "$DB_HOST" in localhost|127.0.0.1|::1) is_local=1 ;; esac
if [ "$is_local" -ne 1 ] && [ "${ALLOW_NON_LOCAL:-0}" != "1" ]; then
    fail "'${DB_NAME}' on ${DB_HOST} does not look like a local database. Set ALLOW_NON_LOCAL=1 to override."
fi

# ─── Pick a psql runner ───────────────────────────────────────────────────────
[ -n "$DB_SSH" ] && FORCE_SSH=1
SSH_TARGET="${DB_SSH:-${DB_SSH_USER:-$(id -un)}@${DB_HOST}}"
SSH_OPTS=(-o BatchMode=yes -o ConnectTimeout=10)

pick_runner() {
    if [ "$FORCE_SSH" -eq 1 ]; then RUNNER=ssh
    elif command -v psql >/dev/null 2>&1; then RUNNER=local
    elif command -v ssh >/dev/null 2>&1; then RUNNER=ssh
    elif command -v docker >/dev/null 2>&1 && docker info >/dev/null 2>&1; then RUNNER=docker
    else fail "No way to run psql (no local psql, ssh or running docker)."
    fi
}
pick_runner

run_sql() {  # reads SQL from stdin
    case "$RUNNER" in
        local)
            PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" \
                -v ON_ERROR_STOP=1 --single-transaction "$@" ;;
        ssh)
            # First stdin line = password (read into PGPASSWORD on the remote side), rest = SQL.
            { printf '%s\n' "$DB_PASSWORD"; cat; } | ssh "${SSH_OPTS[@]}" "$SSH_TARGET" \
                "read -r PGPASSWORD; export PGPASSWORD; exec psql -h localhost -p '$DB_PORT' -U '$DB_USER' \
                 -d '$DB_NAME' -v ON_ERROR_STOP=1 --single-transaction $*" ;;
        docker)
            local host="$DB_HOST"
            case "$host" in localhost|127.0.0.1) host="host.docker.internal" ;; esac
            docker run --rm -i -e PGPASSWORD="$DB_PASSWORD" postgres:16 \
                psql -h "$host" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" \
                -v ON_ERROR_STOP=1 --single-transaction "$@" ;;
    esac
}

info "Seed file : ${SEED_FILE#"$PROJECT_ROOT"/}"
info "Database  : ${DB_NAME} on ${DB_HOST}:${DB_PORT} (user ${DB_USER}) via ${RUNNER}$([ "$RUNNER" = ssh ] && echo " (${SSH_TARGET})")"

if [ "$DRY_RUN" -eq 1 ]; then
    info "Dry run — nothing applied. SQL that would run:"
    echo "-----"; cat "$SEED_FILE"; echo "-----"
    exit 0
fi

run_sql -q < "$SEED_FILE"
success "Seed applied."

# ─── Show the result (only meaningful for the personal therapist seed) ────────
run_sql -q -P pager=off <<'SQL' || true
SELECT u.email,
       string_agg(DISTINCT r.role, ',') AS roles,
       (tp.user_id IS NOT NULL)         AS profile_linked,
       tp.profile_completion_status     AS profile_status
FROM   users u
LEFT   JOIN user_roles r ON r.user_id = u.id
LEFT   JOIN therapist_profile tp ON tp.user_id = u.id
WHERE  u.email = 'alphanet.vin@gmail.com'
GROUP  BY u.email, tp.user_id, tp.profile_completion_status;
SQL
