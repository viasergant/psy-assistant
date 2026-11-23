"""
Role-based and mode-based access control integration tests.
Tests that endpoints reject the wrong roles and unauthenticated callers.
"""
import pytest
from datetime import date, timedelta
from .utils.auth_helper import APIClient, login
from .utils.data_factory import DataFactory


@pytest.fixture(scope="module")
def therapist_client(session_admin_client, reference_data, test_config):
    """
    Create a therapist account, perform first-login password change,
    and return an authenticated APIClient using the therapist's token.
    """
    spec = reference_data["specializations"][0]
    payload = DataFactory.therapist_with_account(
        email=DataFactory.unique_email("rbac-therapist"),
        specialization_id=spec["id"],
    )

    resp = session_admin_client.post(
        "/api/v1/therapists/with-account",
        json=payload,
        expected_status=201,
    )
    temp_password = resp.json()["userDetails"]["temporaryPassword"]
    email = payload["email"]

    client = APIClient(base_url=test_config["api_base_url"])

    # Login with temporary password
    login_resp = client.post(
        "/api/v1/auth/login",
        json={"email": email, "password": temp_password},
        expected_status=200,
    )
    client.set_token(login_resp.json()["accessToken"])

    # Change password to clear mustChangePassword flag
    new_password = "RbacTest123!"
    client.post(
        "/api/v1/auth/first-login-password-change",
        json={"currentPassword": temp_password, "newPassword": new_password},
        allow_error=True,
    )

    # Re-login with the new password
    login_resp2 = client.post(
        "/api/v1/auth/login",
        json={"email": email, "password": new_password},
        allow_error=True,
    )
    if login_resp2.status_code == 200:
        client.set_token(login_resp2.json()["accessToken"])

    return client


def test_therapist_cannot_access_billing(therapist_client):
    """Test that a THERAPIST role is denied access to invoice endpoints (403)."""
    response = therapist_client.get(
        "/api/v1/invoices",
        allow_error=True,
    )

    assert response.status_code == 403, \
        f"THERAPIST should get 403 on /api/v1/invoices, got {response.status_code}"


def test_therapist_cannot_access_admin_users(therapist_client):
    """Test that a THERAPIST role is denied access to admin user management (403)."""
    response = therapist_client.get(
        "/api/v1/admin/users",
        allow_error=True,
    )

    assert response.status_code == 403, \
        f"THERAPIST should get 403 on /api/v1/admin/users, got {response.status_code}"


def test_unauthenticated_cannot_access_clients(unauthenticated_client):
    """Test that unauthenticated requests to the client list return 401."""
    response = unauthenticated_client.get(
        "/api/v1/clients",
        allow_error=True,
    )

    assert response.status_code == 401, "Should return 401 without authentication"


def test_unauthenticated_cannot_access_sessions(unauthenticated_client):
    """Test that unauthenticated requests to the session list return 401."""
    response = unauthenticated_client.get(
        "/api/sessions",
        allow_error=True,
    )

    assert response.status_code == 401, "Should return 401 without authentication"


def test_unauthenticated_cannot_access_billing(unauthenticated_client):
    """Test that unauthenticated requests to invoices return 401."""
    response = unauthenticated_client.get(
        "/api/v1/invoices",
        allow_error=True,
    )

    assert response.status_code == 401, "Should return 401 without authentication"


def test_unauthenticated_cannot_access_reports(unauthenticated_client):
    """Test that unauthenticated requests to reports return 401."""
    date_from = (date.today() - timedelta(days=30)).isoformat()
    date_to = date.today().isoformat()

    response = unauthenticated_client.get(
        "/api/v1/reports/revenue/data",
        params={"dateFrom": date_from, "dateTo": date_to},
        allow_error=True,
    )

    assert response.status_code == 401, "Should return 401 without authentication"


@pytest.mark.solo
def test_solo_mode_blocks_caseload_endpoint(admin_client):
    """Test that the caseload endpoint is blocked in SOLO deployment mode."""
    response = admin_client.get(
        "/api/v1/caseload",
        allow_error=True,
    )

    assert response.status_code != 200, \
        f"Caseload endpoint should not return 200 in SOLO mode, got {response.status_code}"


@pytest.mark.solo
def test_solo_mode_blocks_admin_users_endpoint(admin_client):
    """Test that the admin users endpoint is blocked in SOLO deployment mode."""
    response = admin_client.get(
        "/api/v1/admin/users",
        allow_error=True,
    )

    assert response.status_code != 200, \
        f"Admin users endpoint should not return 200 in SOLO mode, got {response.status_code}"
