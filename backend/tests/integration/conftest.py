"""
Pytest configuration and shared fixtures for integration tests.
"""
import os
import pytest
from dotenv import load_dotenv
from typing import Dict, Any, Generator
from .utils.auth_helper import APIClient, login


# Load environment variables from .env file
load_dotenv(os.path.join(os.path.dirname(__file__), '.env'))


@pytest.fixture(scope="session")
def test_config() -> Dict[str, Any]:
    """
    Load test configuration from environment variables.

    Returns:
        Dictionary with configuration values

    Raises:
        ValueError: If required environment variables are missing
    """
    required_vars = ["API_BASE_URL", "ADMIN_EMAIL", "ADMIN_PASSWORD"]
    missing = [var for var in required_vars if not os.getenv(var)]

    if missing:
        raise ValueError(
            f"Missing required environment variables: {', '.join(missing)}. "
            f"Copy .env.test to .env and configure."
        )

    return {
        "api_base_url": os.getenv("API_BASE_URL", "http://localhost:8080"),
        "request_timeout": int(os.getenv("REQUEST_TIMEOUT", "30")),
        "admin_email": os.getenv("ADMIN_EMAIL"),
        "admin_password": os.getenv("ADMIN_PASSWORD"),
        "test_therapist_email": os.getenv("TEST_THERAPIST_EMAIL"),
        "test_therapist_password": os.getenv("TEST_THERAPIST_PASSWORD"),
        "db_cleanup_strategy": os.getenv("DB_CLEANUP_STRATEGY", "after_each_test"),
        "enable_external_tests": os.getenv("ENABLE_EXTERNAL_TESTS", "false").lower() == "true",
        "test_log_level": os.getenv("TEST_LOG_LEVEL", "INFO")
    }


@pytest.fixture(scope="session")
def api_client(test_config: Dict[str, Any]) -> APIClient:
    """
    Create API client instance for session.

    Args:
        test_config: Test configuration fixture

    Returns:
        Configured APIClient instance
    """
    return APIClient(
        base_url=test_config["api_base_url"],
        timeout=test_config["request_timeout"]
    )


@pytest.fixture(scope="function")
def unauthenticated_client(test_config: Dict[str, Any]) -> APIClient:
    """
    Create unauthenticated API client for testing auth flows.

    Returns:
        APIClient without authentication token
    """
    client = APIClient(
        base_url=test_config["api_base_url"],
        timeout=test_config["request_timeout"]
    )
    # Ensure no token is set
    client.clear_token()
    return client


@pytest.fixture(scope="session")
def admin_token(api_client: APIClient, test_config: Dict[str, Any]) -> str:
    """
    Authenticate as admin and return JWT token.

    Args:
        api_client: Shared API client
        test_config: Test configuration

    Returns:
        Admin JWT access token
    """
    token = login(
        api_client,
        test_config["admin_email"],
        test_config["admin_password"]
    )
    return token


@pytest.fixture(scope="function")
def admin_client(api_client: APIClient, admin_token: str) -> Generator[APIClient, None, None]:
    """
    Provide API client authenticated as admin for individual test.

    Args:
        api_client: Session-scoped API client
        admin_token: Admin JWT token

    Yields:
        Authenticated API client
    """
    # Set admin token
    api_client.set_token(admin_token)

    yield api_client

    # Cleanup after test (token remains set for session)


@pytest.fixture(scope="session")
def reference_data(admin_client: APIClient) -> Dict[str, Any]:
    """
    Load reference data (specializations, languages, session types) once per session.

    Args:
        admin_client: Authenticated admin API client

    Returns:
        Dictionary with reference data lists
    """
    specializations_response = admin_client.get("/api/v1/specializations")
    specializations = specializations_response.json()

    languages_response = admin_client.get("/api/v1/languages")
    languages = languages_response.json()

    session_types_response = admin_client.get("/api/v1/appointments/session-types")
    session_types = session_types_response.json()

    return {
        "specializations": specializations,
        "languages": languages,
        "session_types": session_types
    }


@pytest.fixture(scope="function")
def created_resources() -> Generator[Dict[str, list], None, None]:
    """
    Track resources created during test for cleanup.

    Usage in test:
        created_resources["therapists"].append(therapist_id)

    Yields:
        Dictionary with resource type -> list of IDs
    """
    resources = {
        "therapists": [],
        "leads": [],
        "clients": [],
        "appointments": [],
        "sessions": [],
        "schedules": [],
        "leaves": []
    }

    yield resources

    # Cleanup logic would go here if implementing manual cleanup
    # For now, we rely on test database reset between runs


def pytest_configure(config):
    """
    Configure pytest with custom markers.
    """
    config.addinivalue_line(
        "markers", "external: marks tests requiring external service integration (deselect with '-m \"not external\"')"
    )
    config.addinivalue_line(
        "markers", "slow: marks tests that are slow to run"
    )
    config.addinivalue_line(
        "markers", "e2e: marks end-to-end flow tests"
    )


def pytest_collection_modifyitems(config, items):
    """
    Automatically skip tests marked as 'external' if not enabled.
    """
    skip_external = pytest.mark.skip(reason="External service tests disabled (set ENABLE_EXTERNAL_TESTS=true)")

    enable_external = os.getenv("ENABLE_EXTERNAL_TESTS", "false").lower() == "true"

    if not enable_external:
        for item in items:
            if "external" in item.keywords:
                item.add_marker(skip_external)
