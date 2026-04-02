#!/usr/bin/env bash
# seed-therapist-and-client.sh
#
# Creates a therapist user+profile and a client (via lead conversion) against
# the psy-assistant API.
#
# Required environment variables:
#   API_BASE_URL   e.g. http://localhost:8080
#   ADMIN_EMAIL    admin account email
#   ADMIN_PASSWORD admin account password
#
# Usage:
#   ./seed-therapist-and-client.sh \
#     --therapist-name "Jane Doe" \
#     --therapist-email "jane@clinic.com" \
#     --therapist-specialization "Anxiety" \
#     --client-name "Ivan Petrenko" \
#     --client-email "ivan@example.com"
#
# Optional:
#   --therapist-phone "+380991234567"
#   --employment-status "ACTIVE"   (default: ACTIVE)
#   --client-phone "+380991112233"
#   --lead-source "referral"
#   --lead-notes "Referred by Dr. Smith"

set -euo pipefail

# ─── Colour helpers ───────────────────────────────────────────────────────────
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; BOLD='\033[1m'; RESET='\033[0m'
info()  { echo -e "${BOLD}[INFO]${RESET}  $*"; }
ok()    { echo -e "${GREEN}[OK]${RESET}    $*"; }
warn()  { echo -e "${YELLOW}[WARN]${RESET}  $*"; }
die()   { echo -e "${RED}[ERROR]${RESET} $*" >&2; exit 1; }

# ─── Dependency check ─────────────────────────────────────────────────────────
command -v curl >/dev/null 2>&1 || die "curl is required but not installed."
command -v jq   >/dev/null 2>&1 || die "jq is required but not installed."

# ─── Defaults ─────────────────────────────────────────────────────────────────
THERAPIST_NAME=""
THERAPIST_EMAIL=""
THERAPIST_PHONE=""
THERAPIST_SPECIALIZATION=""
EMPLOYMENT_STATUS="ACTIVE"
CLIENT_NAME=""
CLIENT_EMAIL=""
CLIENT_PHONE=""
LEAD_SOURCE=""
LEAD_NOTES=""

# ─── Argument parsing ─────────────────────────────────────────────────────────
usage() {
  cat <<EOF
Usage: $(basename "$0") [OPTIONS]

Required:
  --therapist-name          Therapist full name
  --therapist-email         Therapist email address
  --therapist-specialization Specialization name (e.g. "Anxiety")
  --client-name             Client full name
  --client-email OR --client-phone  At least one contact method for client

Optional:
  --therapist-phone         Therapist phone number
  --employment-status       ACTIVE | INACTIVE (default: ACTIVE)
  --client-phone            Client phone number
  --lead-source             Lead source (e.g. referral, website)
  --lead-notes              Free-text notes for the lead

Environment variables (required):
  API_BASE_URL   Base URL of the API  (e.g. http://localhost:8080)
  ADMIN_EMAIL    Admin account email
  ADMIN_PASSWORD Admin account password
EOF
  exit 1
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --therapist-name)           THERAPIST_NAME="$2";           shift 2 ;;
    --therapist-email)          THERAPIST_EMAIL="$2";          shift 2 ;;
    --therapist-phone)          THERAPIST_PHONE="$2";          shift 2 ;;
    --therapist-specialization) THERAPIST_SPECIALIZATION="$2"; shift 2 ;;
    --employment-status)        EMPLOYMENT_STATUS="$2";        shift 2 ;;
    --client-name)              CLIENT_NAME="$2";              shift 2 ;;
    --client-email)             CLIENT_EMAIL="$2";             shift 2 ;;
    --client-phone)             CLIENT_PHONE="$2";             shift 2 ;;
    --lead-source)              LEAD_SOURCE="$2";              shift 2 ;;
    --lead-notes)               LEAD_NOTES="$2";               shift 2 ;;
    -h|--help)                  usage ;;
    *) die "Unknown argument: $1. Run with --help for usage." ;;
  esac
done

# ─── Input validation ─────────────────────────────────────────────────────────
[[ -z "${API_BASE_URL:-}" ]]    && die "API_BASE_URL environment variable is not set."
[[ -z "${ADMIN_EMAIL:-}" ]]     && die "ADMIN_EMAIL environment variable is not set."
[[ -z "${ADMIN_PASSWORD:-}" ]]  && die "ADMIN_PASSWORD environment variable is not set."
[[ -z "$THERAPIST_NAME" ]]      && die "--therapist-name is required."
[[ -z "$THERAPIST_EMAIL" ]]     && die "--therapist-email is required."
[[ -z "$THERAPIST_SPECIALIZATION" ]] && die "--therapist-specialization is required."
[[ -z "$CLIENT_NAME" ]]         && die "--client-name is required."
[[ -z "$CLIENT_EMAIL" && -z "$CLIENT_PHONE" ]] && die "At least one of --client-email or --client-phone is required."

# Strip trailing slash from base URL
API_BASE_URL="${API_BASE_URL%/}"

# ─── Helper: make an HTTP call, return body; die on non-2xx ───────────────────
# Usage: http_call METHOD PATH [BODY_JSON]
# Prints response body to stdout.
http_call() {
  local method="$1"
  local path="$2"
  local body="${3:-}"
  local url="${API_BASE_URL}${path}"
  local args=(-s -w "\n%{http_code}" -X "$method" -H "Content-Type: application/json")

  if [[ -n "${TOKEN:-}" ]]; then
    args+=(-H "Authorization: Bearer $TOKEN")
  fi
  if [[ -n "$body" ]]; then
    args+=(-d "$body")
  fi

  local raw
  raw=$(curl "${args[@]}" "$url")

  local http_code
  http_code=$(tail -n1 <<< "$raw")
  local response_body
  response_body=$(sed '$d' <<< "$raw")

  if [[ "$http_code" -lt 200 || "$http_code" -ge 300 ]]; then
    echo -e "${RED}[ERROR]${RESET} HTTP $http_code on $method $path" >&2
    echo "$response_body" | jq . 2>/dev/null || echo "$response_body" >&2
    exit 1
  fi

  echo "$response_body"
}

# ─── Step 1: Admin login ──────────────────────────────────────────────────────
info "Authenticating as admin ($ADMIN_EMAIL)..."

LOGIN_BODY=$(jq -n --arg e "$ADMIN_EMAIL" --arg p "$ADMIN_PASSWORD" \
  '{"email": $e, "password": $p}')

LOGIN_RESPONSE=$(http_call POST "/api/v1/auth/login" "$LOGIN_BODY")
TOKEN=$(jq -r '.accessToken' <<< "$LOGIN_RESPONSE")

[[ -z "$TOKEN" || "$TOKEN" == "null" ]] && die "Failed to obtain access token."
ok "Authenticated."

# ─── Step 2: Resolve specialization name → UUID ───────────────────────────────
info "Fetching specializations..."

SPEC_LIST=$(http_call GET "/api/v1/specializations")

SPECIALIZATION_ID=$(jq -r --arg name "$THERAPIST_SPECIALIZATION" \
  '.[] | select(.name | ascii_downcase == ($name | ascii_downcase)) | .id' \
  <<< "$SPEC_LIST")

if [[ -z "$SPECIALIZATION_ID" || "$SPECIALIZATION_ID" == "null" ]]; then
  warn "Specialization \"$THERAPIST_SPECIALIZATION\" not found. Available specializations:"
  jq -r '.[].name' <<< "$SPEC_LIST" | sort | while read -r name; do
    echo "    - $name"
  done
  die "Please re-run with a valid --therapist-specialization value."
fi
ok "Resolved specialization \"$THERAPIST_SPECIALIZATION\" → $SPECIALIZATION_ID"

# ─── Step 3: Create therapist with account ────────────────────────────────────
info "Creating therapist account + profile for $THERAPIST_EMAIL..."

THERAPIST_BODY=$(jq -n \
  --arg email "$THERAPIST_EMAIL" \
  --arg fullName "$THERAPIST_NAME" \
  --arg phone "$THERAPIST_PHONE" \
  --arg status "$EMPLOYMENT_STATUS" \
  --arg specId "$SPECIALIZATION_ID" \
  '{
    email: $email,
    fullName: $fullName,
    employmentStatus: $status,
    primarySpecializationId: $specId
  } | if $phone != "" then . + {phone: $phone} else . end')

THERAPIST_RESPONSE=$(http_call POST "/api/v1/therapists/with-account" "$THERAPIST_BODY")

THERAPIST_PROFILE_ID=$(jq -r '.therapistProfile.id' <<< "$THERAPIST_RESPONSE")
THERAPIST_TEMP_PW=$(jq -r '.userDetails.temporaryPassword' <<< "$THERAPIST_RESPONSE")

ok "Therapist created (profile ID: $THERAPIST_PROFILE_ID)"

# ─── Step 4: Build contact methods array ──────────────────────────────────────
CONTACT_METHODS="[]"
FIRST=true

if [[ -n "$CLIENT_EMAIL" ]]; then
  CONTACT_METHODS=$(jq -n \
    --arg v "$CLIENT_EMAIL" \
    '[{"type":"EMAIL","value":$v,"isPrimary":true}]')
  FIRST=false
fi

if [[ -n "$CLIENT_PHONE" ]]; then
  IS_PRIMARY="$([[ "$FIRST" == true ]] && echo "true" || echo "false")"
  CONTACT_METHODS=$(jq -n \
    --argjson existing "$CONTACT_METHODS" \
    --arg v "$CLIENT_PHONE" \
    --argjson primary "$IS_PRIMARY" \
    '$existing + [{"type":"PHONE","value":$v,"isPrimary":$primary}]')
fi

# ─── Step 5: Create lead (status = NEW) ───────────────────────────────────────
info "Creating lead for $CLIENT_NAME..."

LEAD_BODY=$(jq -n \
  --arg fullName "$CLIENT_NAME" \
  --arg source "$LEAD_SOURCE" \
  --arg notes "$LEAD_NOTES" \
  --argjson contactMethods "$CONTACT_METHODS" \
  '{
    fullName: $fullName,
    contactMethods: $contactMethods
  }
  | if $source != "" then . + {source: $source} else . end
  | if $notes  != "" then . + {notes: $notes}  else . end')

LEAD_RESPONSE=$(http_call POST "/api/v1/leads" "$LEAD_BODY")
LEAD_ID=$(jq -r '.id' <<< "$LEAD_RESPONSE")
ok "Lead created (ID: $LEAD_ID, status: NEW)"

# ─── Step 6: Transition NEW → CONTACTED ───────────────────────────────────────
info "Transitioning lead to CONTACTED..."
http_call PATCH "/api/v1/leads/$LEAD_ID/status" '{"status":"CONTACTED"}' > /dev/null
ok "Lead status: CONTACTED"

# ─── Step 7: Transition CONTACTED → QUALIFIED ─────────────────────────────────
info "Transitioning lead to QUALIFIED..."
http_call PATCH "/api/v1/leads/$LEAD_ID/status" '{"status":"QUALIFIED"}' > /dev/null
ok "Lead status: QUALIFIED"

# ─── Step 8: Convert lead → client ────────────────────────────────────────────
info "Converting lead to client..."

CONVERT_BODY=$(jq -n \
  --arg fullName "$CLIENT_NAME" \
  --arg notes "$LEAD_NOTES" \
  --argjson contactMethods "$CONTACT_METHODS" \
  '{
    fullName: $fullName,
    contactMethods: $contactMethods
  }
  | if $notes != "" then . + {notes: $notes} else . end')

CONVERT_RESPONSE=$(http_call POST "/api/v1/leads/$LEAD_ID/convert" "$CONVERT_BODY")
CLIENT_ID=$(jq -r '.clientId' <<< "$CONVERT_RESPONSE")
ok "Lead converted to client (client ID: $CLIENT_ID)"

# ─── Step 9: Create therapist schedule ────────────────────────────────────────
info "Creating therapist schedule for today..."

# Get current day of week (1=Monday, 7=Sunday)
CURRENT_DAY=$(date +%u)

SCHEDULE_BODY=$(jq -n \
  --argjson dayOfWeek "$CURRENT_DAY" \
  '{
    dayOfWeek: $dayOfWeek,
    startTime: "09:00:00",
    endTime: "17:00:00",
    timezone: "Europe/Kiev"
  }')

SCHEDULE_RESPONSE=$(http_call POST "/api/v1/therapists/$THERAPIST_PROFILE_ID/schedule/recurring" "$SCHEDULE_BODY")
SCHEDULE_ID=$(jq -r '.id' <<< "$SCHEDULE_RESPONSE")
ok "Schedule created (ID: $SCHEDULE_ID, day: $CURRENT_DAY)"

# ─── Step 10: Fetch session types ─────────────────────────────────────────────
info "Fetching available session types..."

SESSION_TYPES=$(http_call GET "/api/v1/appointments/session-types")

# Use first available session type
SESSION_TYPE_ID=$(jq -r '.[0].id' <<< "$SESSION_TYPES")
SESSION_TYPE_NAME=$(jq -r '.[0].name' <<< "$SESSION_TYPES")

if [[ -z "$SESSION_TYPE_ID" || "$SESSION_TYPE_ID" == "null" ]]; then
  die "No session types available in the system."
fi
ok "Using session type: $SESSION_TYPE_NAME (ID: $SESSION_TYPE_ID)"

# ─── Step 11: Create appointment for today ────────────────────────────────────
info "Creating appointment for today at 14:00..."

# Get current date and time at 14:00 in Europe/Kiev timezone
APPOINTMENT_TIME=$(TZ=Europe/Kiev date -Iseconds -d "today 14:00" 2>/dev/null || date -u -v14H -v0M -v0S +"%Y-%m-%dT%H:%M:%S%z")

APPOINTMENT_BODY=$(jq -n \
  --arg therapistId "$THERAPIST_PROFILE_ID" \
  --arg clientId "$CLIENT_ID" \
  --arg sessionTypeId "$SESSION_TYPE_ID" \
  --arg startTime "$APPOINTMENT_TIME" \
  '{
    therapistProfileId: $therapistId,
    clientId: $clientId,
    sessionTypeId: $sessionTypeId,
    startTime: $startTime,
    durationMinutes: 60,
    timezone: "Europe/Kiev",
    notes: "Initial consultation session",
    allowConflictOverride: false
  }')

APPOINTMENT_RESPONSE=$(http_call POST "/api/v1/appointments" "$APPOINTMENT_BODY")
APPOINTMENT_ID=$(jq -r '.id' <<< "$APPOINTMENT_RESPONSE")
APPOINTMENT_STATUS=$(jq -r '.status' <<< "$APPOINTMENT_RESPONSE")
ok "Appointment created (ID: $APPOINTMENT_ID, status: $APPOINTMENT_STATUS)"

# ─── Step 12: Start session ───────────────────────────────────────────────────
info "Starting session for appointment..."

SESSION_START_BODY=$(jq -n \
  --arg appointmentId "$APPOINTMENT_ID" \
  '{
    appointmentId: $appointmentId
  }')

SESSION_RESPONSE=$(http_call POST "/api/sessions/start" "$SESSION_START_BODY")
SESSION_ID=$(jq -r '.id' <<< "$SESSION_RESPONSE")
SESSION_STATUS=$(jq -r '.status' <<< "$SESSION_RESPONSE")
ok "Session started (ID: $SESSION_ID, status: $SESSION_STATUS)"

# ─── Step 13: Summary ─────────────────────────────────────────────────────────
echo ""
echo -e "${BOLD}─── Summary ───────────────────────────────────────────────────────${RESET}"
echo -e "${GREEN}Therapist created${RESET}"
echo "  Name:        $THERAPIST_NAME"
echo "  Email:       $THERAPIST_EMAIL"
echo "  Profile ID:  $THERAPIST_PROFILE_ID"
echo "  Temp PW:     $THERAPIST_TEMP_PW"
echo ""
echo -e "${GREEN}Client created${RESET}"
echo "  Name:        $CLIENT_NAME"
echo "  Client ID:   $CLIENT_ID"
echo "  Lead ID:     $LEAD_ID"
echo ""
echo -e "${GREEN}Schedule created${RESET}"
echo "  Schedule ID: $SCHEDULE_ID"
echo "  Day:         Day $CURRENT_DAY (09:00-17:00)"
echo ""
echo -e "${GREEN}Appointment & Session${RESET}"
echo "  Appointment ID: $APPOINTMENT_ID"
echo "  Session ID:     $SESSION_ID"
echo "  Time:           $APPOINTMENT_TIME"
echo "  Status:         $SESSION_STATUS"
echo "  Session Type:   $SESSION_TYPE_NAME"
echo -e "${BOLD}───────────────────────────────────────────────────────────────────${RESET}"
echo ""
echo "The therapist must change their password on first login:"
echo "  POST /api/v1/auth/first-login-password-change"
echo "  Body: { \"currentPassword\": \"<Temp PW>\", \"newPassword\": \"<new 10+ char password>\" }"
