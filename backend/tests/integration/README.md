# Integration Tests

Python-based integration test suite for PSY-ASSISTANT API. Tests validate end-to-end workflows, API contracts, authentication, authorization, and cross-feature interactions against a running backend instance.

## Prerequisites

- **Python 3.9+** installed
- **Backend running** locally or on a test server
- **Database access** (test database recommended)
- **Admin credentials** for test setup

## Quick Start

1. **Copy environment template:**
   ```bash
   cd backend/tests/integration
   cp .env.test .env
   ```

2. **Configure `.env` file:**
   ```bash
   # Edit .env with your test environment settings
   API_BASE_URL=http://localhost:8080
   ADMIN_EMAIL=admin@psyassistant.com
   ADMIN_PASSWORD=your_admin_password
   ```

3. **Run tests:**
   ```bash
   # From project root
   ./scripts/integration-test.sh

   # Or directly with pytest
   cd backend/tests/integration
   python3 -m venv .venv
   source .venv/bin/activate
   pip install -r requirements.txt
   pytest -v
   ```

## Test Organization

```
backend/tests/integration/
├── conftest.py                         # Shared fixtures and pytest config
├── requirements.txt                    # Python dependencies
├── .env.test                          # Environment template
├── .env                               # Your local config (git-ignored)
├── utils/
│   ├── auth_helper.py                 # JWT authentication utilities
│   └── data_factory.py                # Test data generators
├── test_auth.py                       # Authentication flows
├── test_users.py                      # User management
├── test_therapist_profile.py          # Therapist CRUD operations
├── test_therapist_schedule.py         # Recurring schedules & overrides
├── test_leave_management.py           # Leave request workflow
├── test_lead_to_client_conversion.py  # Lead conversion flow
├── test_e2e_flows.py                  # End-to-end user journeys
└── test_error_handling.py             # Error cases & validation
```

## Running Tests

### Run All Tests
```bash
./scripts/integration-test.sh
```

### Run Specific Test File
```bash
./scripts/integration-test.sh test_auth.py
```

### Run Specific Test
```bash
./scripts/integration-test.sh -k "test_login_with_valid_admin_credentials"
```

### Run with Verbose Output
```bash
./scripts/integration-test.sh -v
```

### Run in Parallel
```bash
./scripts/integration-test.sh -n auto
```

### Run E2E Tests Only
```bash
./scripts/integration-test.sh -m e2e
```

### Skip External Service Tests
```bash
./scripts/integration-test.sh -m "not external"
```

### Generate HTML Report
```bash
./scripts/integration-test.sh --html=reports/report.html --self-contained-html
```

### Run with Debug Logging
```bash
./scripts/integration-test.sh --log-cli-level=DEBUG
```

## Test Fixtures

### Configuration
- **`test_config`**: Loads environment variables and validates required settings
- **`api_client`**: Session-scoped HTTP client for API requests

### Authentication
- **`unauthenticated_client`**: API client without token (for auth tests)
- **`admin_token`**: JWT token for admin user
- **`admin_client`**: Authenticated API client as admin

### Reference Data
- **`reference_data`**: Session-scoped cache of specializations, languages, session types

### Resource Tracking
- **`created_resources`**: Tracks entities created during test for cleanup

## Environment Variables

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `API_BASE_URL` | ✅ | - | Base URL of backend API |
| `ADMIN_EMAIL` | ✅ | - | Admin account email |
| `ADMIN_PASSWORD` | ✅ | - | Admin account password |
| `REQUEST_TIMEOUT` | ❌ | 30 | HTTP request timeout (seconds) |
| `DB_CLEANUP_STRATEGY` | ❌ | after_each_test | When to clean test data |
| `ENABLE_EXTERNAL_TESTS` | ❌ | false | Run tests requiring external services |
| `TEST_LOG_LEVEL` | ❌ | INFO | Logging verbosity |

## Test Markers

Use markers to selectively run test categories:

```bash
# Run only e2e tests
pytest -m e2e

# Skip slow tests
pytest -m "not slow"

# Skip external service tests
pytest -m "not external"
```

Available markers:
- **`@pytest.mark.e2e`**: End-to-end flow tests
- **`@pytest.mark.slow`**: Tests that take longer to run
- **`@pytest.mark.external`**: Tests requiring external services (auto-skipped unless enabled)

## Writing Tests

### Example: Simple API Test
```python
def test_get_therapist_list(admin_client):
    """Test retrieving paginated therapist list."""
    response = admin_client.get(
        "/api/v1/therapists",
        params={"page": 0, "size": 10},
        expected_status=200
    )

    data = response.json()
    assert "content" in data
    assert isinstance(data["content"], list)
```

### Example: Using Data Factory
```python
from utils.data_factory import DataFactory

def test_create_therapist(admin_client, reference_data):
    """Test creating therapist with generated data."""
    specialization = reference_data["specializations"][0]

    payload = DataFactory.therapist_with_account(
        email=DataFactory.unique_email("test"),
        full_name="Dr. Test",
        specialization_id=specialization["id"]
    )

    response = admin_client.post(
        "/api/v1/therapists/with-account",
        json=payload,
        expected_status=201
    )

    assert response.json()["therapistProfile"]["fullName"] == "Dr. Test"
```

### Example: Error Handling Test
```python
def test_invalid_email_returns_400(admin_client):
    """Test that invalid email format is rejected."""
    invalid_payload = {
        "email": "not-an-email",
        "fullName": "Test",
        "employmentStatus": "ACTIVE",
        "primarySpecializationId": "some-uuid"
    }

    response = admin_client.post(
        "/api/v1/therapists/with-account",
        json=invalid_payload,
        allow_error=True
    )

    assert response.status_code == 400
```

## Troubleshooting

### Backend Not Running
**Error:** `Health check failed`

**Solution:**
```bash
# Start backend locally
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

### Authentication Failures
**Error:** `Failed to obtain access token`

**Solution:** Verify admin credentials in `.env` match existing user in database

### Test Data Conflicts
**Error:** `409 Conflict` or duplicate constraint violations

**Solution:** Ensure unique email/identifiers in test data or enable DB cleanup

### Import Errors
**Error:** `ModuleNotFoundError: No module named 'utils'`

**Solution:** Ensure virtual environment is activated and dependencies installed:
```bash
cd backend/tests/integration
source .venv/bin/activate
pip install -r requirements.txt
```

## CI Integration

Tests can be integrated into CI/CD pipelines:

### GitHub Actions Example
```yaml
name: Integration Tests

on: [push, pull_request]

jobs:
  integration:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Start Backend
        run: |
          cd backend
          docker compose up -d
          ./mvnw spring-boot:run -Dspring-boot.run.profiles=local &
          sleep 30

      - name: Wait for Health
        run: |
          curl --retry 10 --retry-delay 5 http://localhost:8080/actuator/health

      - name: Run Integration Tests
        env:
          API_BASE_URL: http://localhost:8080
          ADMIN_EMAIL: ${{ secrets.ADMIN_EMAIL }}
          ADMIN_PASSWORD: ${{ secrets.ADMIN_PASSWORD }}
        run: ./scripts/integration-test.sh

      - name: Upload Test Report
        if: always()
        uses: actions/upload-artifact@v3
        with:
          name: integration-test-report
          path: backend/tests/integration/reports/
```

## Best Practices

1. **Use unique identifiers** for test data to avoid conflicts (e.g., `DataFactory.unique_email()`)
2. **Track created resources** in `created_resources` fixture for potential cleanup
3. **Test both success and failure paths** for robust coverage
4. **Use descriptive test names** that explain what is being validated
5. **Leverage fixtures** to reduce duplication and improve readability
6. **Validate complete responses**, not just status codes
7. **Test authorization** as well as functionality
8. **Keep tests independent** – avoid inter-test dependencies

## Coverage Goals

- **80%+ endpoint coverage** for public API
- **100% coverage** for critical flows:
  - Authentication and authorization
  - Lead to client conversion
  - Appointment scheduling and conflicts
  - Leave approval workflow
  - Therapist onboarding

## Related Documentation

- [Backend README](../../README.md) – Backend setup and local development
- [Project Guidelines](../../../AGENTS.md) – Overall project conventions
- [API Controllers](../../src/main/java/com/psyassistant/*/rest/) – REST API implementation
