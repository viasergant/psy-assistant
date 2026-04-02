#!/usr/bin/env bash
# integration-test.sh
#
# Run Python integration tests against psy-assistant API
#
# Prerequisites:
#   - Python 3.9+ installed
#   - Backend running (local or remote)
#   - .env file configured in backend/tests/integration/
#
# Usage:
#   ./scripts/integration-test.sh [pytest args]
#
# Examples:
#   ./scripts/integration-test.sh                          # Run all tests
#   ./scripts/integration-test.sh -v                       # Verbose output
#   ./scripts/integration-test.sh -n auto                  # Parallel execution
#   ./scripts/integration-test.sh test_auth.py             # Run specific file
#   ./scripts/integration-test.sh -k "test_login"          # Run tests matching pattern
#   ./scripts/integration-test.sh -m "not external"        # Skip external service tests
#   ./scripts/integration-test.sh --html=report.html       # Generate HTML report

set -euo pipefail

# ─── Configuration ────────────────────────────────────────────────────────────
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
TEST_DIR="$PROJECT_ROOT/backend/tests/integration"
VENV_DIR="$TEST_DIR/.venv"
ENV_FILE="$TEST_DIR/.env"
ENV_TEMPLATE="$TEST_DIR/.env.test"

# ─── Colour helpers ───────────────────────────────────────────────────────────
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; BOLD='\033[1m'; RESET='\033[0m'
info()  { echo -e "${BOLD}[INFO]${RESET}  $*"; }
ok()    { echo -e "${GREEN}[OK]${RESET}    $*"; }
warn()  { echo -e "${YELLOW}[WARN]${RESET}  $*"; }
die()   { echo -e "${RED}[ERROR]${RESET} $*" >&2; exit 1; }

# ─── Python version check ─────────────────────────────────────────────────────
if ! command -v python3 &> /dev/null; then
    die "Python 3 is required but not installed."
fi

PYTHON_VERSION=$(python3 --version | awk '{print $2}')
info "Using Python $PYTHON_VERSION"

# ─── Check if backend is running ──────────────────────────────────────────────
check_backend() {
    local api_url="${API_BASE_URL:-http://localhost:8080}"
    local health_endpoint="$api_url/actuator/health"

    info "Checking backend health at $health_endpoint..."

    if curl -sf "$health_endpoint" > /dev/null 2>&1; then
        ok "Backend is running"
        return 0
    else
        warn "Backend health check failed. Ensure backend is running at $api_url"
        return 1
    fi
}

# ─── Virtual environment setup ────────────────────────────────────────────────
setup_venv() {
    if [ ! -d "$VENV_DIR" ]; then
        info "Creating Python virtual environment..."
        python3 -m venv "$VENV_DIR"
        ok "Virtual environment created"
    fi

    info "Activating virtual environment..."
    # shellcheck disable=SC1091
    source "$VENV_DIR/bin/activate"

    info "Installing/updating dependencies..."
    pip install --quiet --upgrade pip
    pip install --quiet -r "$TEST_DIR/requirements.txt"
    ok "Dependencies installed"
}

# ─── Environment configuration check ──────────────────────────────────────────
check_env_config() {
    if [ ! -f "$ENV_FILE" ]; then
        warn ".env file not found at $ENV_FILE"
        if [ -f "$ENV_TEMPLATE" ]; then
            info "Copy .env.test to .env and configure:"
            echo "  cp $ENV_TEMPLATE $ENV_FILE"
            echo "  vim $ENV_FILE"
        fi
        die "Configuration required before running tests"
    fi
    ok "Configuration file found"
}

# ─── Main ─────────────────────────────────────────────────────────────────────
main() {
    cd "$TEST_DIR"

    echo ""
    echo -e "${BOLD}═══════════════════════════════════════════════════════════${RESET}"
    echo -e "${BOLD}  PSY-ASSISTANT Integration Test Suite${RESET}"
    echo -e "${BOLD}═══════════════════════════════════════════════════════════${RESET}"
    echo ""

    check_env_config
    check_backend || warn "Proceeding anyway (tests may fail if backend is down)"
    setup_venv

    echo ""
    info "Running pytest with arguments: ${*:-<none>}"
    echo ""

    # Default pytest arguments if none provided
    PYTEST_ARGS=(
        "-v"                          # Verbose output
        "--tb=short"                  # Short traceback format
        "--color=yes"                 # Colored output
        "--strict-markers"            # Fail on unknown markers
    )

    # Add user arguments or defaults
    if [ $# -eq 0 ]; then
        PYTEST_ARGS+=(
            "--html=reports/integration-test-report.html"
            "--self-contained-html"
            "-m" "not external"       # Skip external tests by default
        )
    else
        PYTEST_ARGS+=("$@")
    fi

    # Create reports directory
    mkdir -p "$TEST_DIR/reports"

    # Run pytest
    pytest "${PYTEST_ARGS[@]}"
    TEST_EXIT_CODE=$?

    echo ""
    if [ $TEST_EXIT_CODE -eq 0 ]; then
        echo -e "${GREEN}${BOLD}✓ All tests passed${RESET}"
    else
        echo -e "${RED}${BOLD}✗ Some tests failed (exit code: $TEST_EXIT_CODE)${RESET}"
    fi

    echo ""
    if [ -f "$TEST_DIR/reports/integration-test-report.html" ]; then
        info "HTML report: $TEST_DIR/reports/integration-test-report.html"
    fi

    exit $TEST_EXIT_CODE
}

main "$@"