"""
Admin user management integration tests (CLINIC mode only).
Tests user listing, creation, role updates, and password reset.
"""
import pytest
from .utils.data_factory import DataFactory


@pytest.mark.clinic
def test_list_users(admin_client):
    """Test listing all users returns paginated result with at least the admin user."""
    response = admin_client.get(
        "/api/v1/admin/users",
        expected_status=200,
    )

    data = response.json()
    # Response may be paginated (content field) or a bare list
    if isinstance(data, list):
        assert len(data) >= 1, "Should contain at least the seeded admin user"
    else:
        assert "content" in data, "Paginated response should contain content"
        assert len(data["content"]) >= 1, "Should contain at least the seeded admin user"


@pytest.mark.clinic
def test_create_user(admin_client):
    """Test creating a new RECEPTION_ADMIN_STAFF user returns the created user with credentials."""
    payload = {
        "email": DataFactory.unique_email("newuser"),
        "fullName": "Test Reception Staff",
        "roles": ["RECEPTION_ADMIN_STAFF"],
    }

    response = admin_client.post(
        "/api/v1/admin/users",
        json=payload,
        expected_status=201,
    )

    data = response.json()
    assert data.get("id"), "Created user should have an ID"
    assert data.get("email") == payload["email"], "Email should match"


@pytest.mark.clinic
def test_create_user_duplicate_email_returns_conflict(admin_client):
    """Test creating a user with an already-registered email is rejected."""
    email = DataFactory.unique_email("dupuser")
    payload = {
        "email": email,
        "fullName": "Duplicate User",
        "roles": ["RECEPTION_ADMIN_STAFF"],
    }

    admin_client.post("/api/v1/admin/users", json=payload, expected_status=201)

    duplicate_response = admin_client.post(
        "/api/v1/admin/users",
        json=payload,
        allow_error=True,
    )

    assert duplicate_response.status_code in (400, 409), \
        "Duplicate email should return 400 or 409"


@pytest.mark.clinic
def test_edit_user_role(admin_client):
    """Test updating a user's assigned roles."""
    create_payload = {
        "email": DataFactory.unique_email("rolechange"),
        "fullName": "Role Change User",
        "roles": ["RECEPTION_ADMIN_STAFF"],
    }

    create_response = admin_client.post(
        "/api/v1/admin/users",
        json=create_payload,
        expected_status=201,
    )
    user_id = create_response.json()["id"]

    update_response = admin_client.patch(
        f"/api/v1/admin/users/{user_id}",
        json={"roles": ["FINANCE"]},
        expected_status=200,
    )

    updated = update_response.json()
    roles = updated.get("roles", [])
    # Roles may be returned as strings or objects; check for FINANCE presence
    role_values = [r if isinstance(r, str) else r.get("name", r) for r in roles]
    assert any("FINANCE" in str(r) for r in role_values), \
        f"Updated roles should contain FINANCE, got: {roles}"


@pytest.mark.clinic
def test_reset_user_password(admin_client):
    """Test initiating a password reset for a user succeeds."""
    create_payload = {
        "email": DataFactory.unique_email("pwreset"),
        "fullName": "Password Reset User",
        "roles": ["RECEPTION_ADMIN_STAFF"],
    }

    create_response = admin_client.post(
        "/api/v1/admin/users",
        json=create_payload,
        expected_status=201,
    )
    user_id = create_response.json()["id"]

    reset_response = admin_client.post(
        f"/api/v1/admin/users/{user_id}/password-reset",
        allow_error=True,
    )

    assert reset_response.status_code in (200, 204), \
        f"Password reset should return 200 or 204, got {reset_response.status_code}"


@pytest.mark.clinic
def test_user_management_requires_authentication(unauthenticated_client):
    """Test that user management endpoints require authentication."""
    response = unauthenticated_client.get(
        "/api/v1/admin/users",
        allow_error=True,
    )

    assert response.status_code == 401, \
        "Should return 401 without authentication"
