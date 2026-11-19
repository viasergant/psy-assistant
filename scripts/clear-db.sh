#!/usr/bin/env bash
# clear-db.sh
#
# Deletes all clients, therapists, and their related transactional data
# while preserving reference/lookup tables and SYSTEM_ADMINISTRATOR accounts.
#
# Preserved tables (unchanged):
#   specialization, language, service_type, session_type, note_template,
#   outcome_measure_definition, risk_flag_types, service_catalog,
#   service_catalog_price_history, prepaid_package_definition,
#   flyway_schema_history, audit_log
#
# Usage (from project root):
#   ./scripts/clear-db.sh
#
# Prerequisites:
#   - Docker Compose postgres service is running (cd backend && docker-compose up -d postgres)

set -euo pipefail

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; BOLD='\033[1m'; RESET='\033[0m'

info()    { echo -e "${BOLD}[INFO]${RESET}  $*"; }
success() { echo -e "${GREEN}[OK]${RESET}    $*"; }
warn()    { echo -e "${YELLOW}[WARN]${RESET}  $*"; }

# ─── Configuration ────────────────────────────────────────────────────────────
DB_NAME="${DB_NAME:-psyassistant}"
DB_USER="${DB_USER:-psyassistant}"
DB_PASSWORD="${DB_PASSWORD:-psyassistant}"
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# Load .env overrides if present
if [ -f "$PROJECT_ROOT/.env" ]; then
    set -a
    # shellcheck disable=SC1091
    source "$PROJECT_ROOT/.env"
    set +a
fi

# ─── Safety prompt ────────────────────────────────────────────────────────────
warn "This will DELETE all clients, therapists, and related data in '${DB_NAME}' on ${DB_HOST}:${DB_PORT}."
warn "SYSTEM_ADMINISTRATOR accounts and reference/lookup tables are preserved."
read -r -p "$(echo -e "${YELLOW}Are you sure? [y/N]:${RESET} ")" CONFIRM
if [[ ! "$CONFIRM" =~ ^[Yy]$ ]]; then
    info "Aborted."
    exit 0
fi

# ─── Helper ───────────────────────────────────────────────────────────────────
psql_cmd() {
    PGPASSWORD="$DB_PASSWORD" psql \
        -h "$DB_HOST" -p "$DB_PORT" \
        -U "$DB_USER" \
        "$@"
}

# ─── Clear data ───────────────────────────────────────────────────────────────
info "Clearing clients, therapists, and related data..."

psql_cmd -d "$DB_NAME" <<'SQL'
BEGIN;

-- Bypass FK constraint enforcement so we can delete in logical groupings
-- without needing to precisely order every child table.
SET LOCAL session_replication_role = replica;

-- ── Audit / append-only logs ──────────────────────────────────────────────────
TRUNCATE
    attendance_audit_log,
    appointment_audit,
    care_plan_audit,
    risk_flag_audit_log,
    therapist_profile_audit_change,
    therapist_profile_audit_entry,
    therapist_schedule_audit_change,
    therapist_schedule_audit_entry,
    client_profile_audit_change,
    client_profile_audit_entry,
    session_participant_audit;

-- ── Session notes & group session data ───────────────────────────────────────
TRUNCATE
    session_note_version,
    session_note,
    group_session_attendance,
    session_participant;

-- ── Session records ───────────────────────────────────────────────────────────
TRUNCATE session_follow_up_tasks, session_record;

-- ── Appointments ──────────────────────────────────────────────────────────────
TRUNCATE appointment, appointment_series;

-- ── Care plans ────────────────────────────────────────────────────────────────
TRUNCATE
    outcome_measure_entry,
    goal_progress_note,
    care_plan_intervention,
    care_plan_milestone,
    care_plan_goal,
    care_plan;

-- ── Financial ─────────────────────────────────────────────────────────────────
TRUNCATE
    package_balance_audit,
    prepaid_package_instance,
    invoice_line_item_discounts,
    invoice_line_items,
    payments,
    refunds,
    wallet_transactions,
    client_wallets,
    invoices,
    invoice_number_seq;

-- CLIENT-scoped discount rules only (SERVICE-scoped ones belong to the catalog)
DELETE FROM discount_rules WHERE scope = 'CLIENT';

-- ── Clients & leads ───────────────────────────────────────────────────────────
TRUNCATE
    client_risk_flags,
    client_tags,
    client_contact_methods,
    clients,
    lead_contact_methods,
    leads;

-- ── Therapist profiles (all child rows covered by the TRUNCATE above) ─────────
TRUNCATE
    supervisor_team_member,
    caseload_snapshot,
    therapist_leave,
    therapist_schedule_override,
    therapist_recurring_schedule,
    therapist_language,
    therapist_specialization,
    therapist_photo,
    therapist_pricing_rule,
    therapist_credential,
    therapist_profile;

-- ── Non-admin users (keep SYSTEM_ADMINISTRATOR accounts) ─────────────────────
-- Identify users to remove: anyone without a SYSTEM_ADMINISTRATOR role
CREATE TEMP TABLE _users_to_delete AS
    SELECT id FROM users
    WHERE id NOT IN (
        SELECT user_id FROM user_roles WHERE role = 'SYSTEM_ADMINISTRATOR'
    );

DELETE FROM service_catalog_therapist_override
    WHERE therapist_id IN (SELECT id FROM _users_to_delete);

DELETE FROM refresh_tokens
    WHERE user_id IN (SELECT id FROM _users_to_delete);

DELETE FROM password_reset_tokens
    WHERE user_id IN (SELECT id FROM _users_to_delete);

DELETE FROM user_roles
    WHERE user_id IN (SELECT id FROM _users_to_delete);

DELETE FROM users
    WHERE id IN (SELECT id FROM _users_to_delete);

DROP TABLE _users_to_delete;

COMMIT;
SQL

success "Done. Clients, therapists, and related data have been cleared."
info  "Reference tables and SYSTEM_ADMINISTRATOR accounts are untouched."
