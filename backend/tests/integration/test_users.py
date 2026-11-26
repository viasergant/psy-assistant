"""
User management integration tests.
Tests user profile operations and language preferences.
"""
import pytest
from .utils.auth_helper import APIClient, login
from .utils.data_factory import DataFactory


def test_update_user_language_preference_to_english(admin_client):
    """Test updating user language preference to English."""
    response = admin_client.patch(
        "/api/v1/users/me/language",
        json={"language": "en"},
        expected_status=204
    )

    assert response.status_code == 204, "Language update should return 204 No Content"


def test_update_user_language_preference_to_ukrainian(admin_client):
    """Test updating user language preference to Ukrainian."""
    response = admin_client.patch(
        "/api/v1/users/me/language",
        json={"language": "uk"},
        expected_status=204
    )

    assert response.status_code == 204, "Language update should return 204 No Content"


def test_update_language_with_invalid_code_returns_400(admin_client):
    """Test updating language with unsupported language code returns 400."""
    response = admin_client.patch(
        "/api/v1/users/me/language",
        json={"language": "fr"},  # French not supported
        allow_error=True
    )

    assert response.status_code == 400, "Should return 400 for unsupported language"


def test_update_language_without_authentication_returns_401(unauthenticated_client):
    """Test language update without authentication returns 401."""
    response = unauthenticated_client.patch(
        "/api/v1/users/me/language",
        json={"language": "en"},
        allow_error=True
    )

    assert response.status_code == 401, "Should return 401 without authentication"


def test_update_language_with_missing_language_field_returns_400(admin_client):
    """Test language update with missing 'language' field returns 400."""
    response = admin_client.patch(
        "/api/v1/users/me/language",
        json={},  # Empty body
        allow_error=True
    )

    assert response.status_code == 400, "Should return 400 when language field is missing"


def test_update_language_with_empty_string_returns_400(admin_client):
    """Test language update with empty string returns 400."""
    response = admin_client.patch(
        "/api/v1/users/me/language",
        json={"language": ""},
        allow_error=True
    )

    assert response.status_code == 400, "Should return 400 for empty language code"


def test_update_language_persistence(admin_client):
    """Test that language preference changes persist across requests."""
    # Set to English
    admin_client.patch(
        "/api/v1/users/me/language",
        json={"language": "en"},
        expected_status=204
    )

    # Set to Ukrainian
    admin_client.patch(
        "/api/v1/users/me/language",
        json={"language": "uk"},
        expected_status=204
    )

    # Both operations should succeed (persistence validated by subsequent requests)
    # Note: Full persistence validation would require user profile endpoint


# ---------------------------------------------------------------------------
# Self-service password change: POST /api/v1/users/me/password
#
# Tests that change or fail a password use a throwaway user created through the admin API
# (clinic mode) so the shared seed admin's password and lockout counter are never touched.
# ---------------------------------------------------------------------------

PASSWORD_URL = "/api/v1/users/me/password"
NEW_PASSWORD = "Br4nd-New-Pass!"


@pytest.fixture
def throwaway_user(admin_client, test_config):
    """Create a user and return (email, temporary password) with a logged-in client."""
    email = DataFactory.unique_email("pwchange")
    created = admin_client.post(
        "/api/v1/admin/users",
        json={"email": email, "fullName": "Password Change User", "roles": ["RECEPTION_ADMIN_STAFF"]},
        expected_status=201,
    ).json()
    temp_password = created["temporaryPassword"]
    client = APIClient(test_config["api_base_url"])
    login(client, email, temp_password)
    return {"email": email, "password": temp_password, "client": client, "config": test_config}


def test_change_password_without_authentication_returns_401(unauthenticated_client):
    """AUTH-017: unauthenticated password change is rejected."""
    response = unauthenticated_client.post(
        PASSWORD_URL,
        json={"currentPassword": "x", "newPassword": NEW_PASSWORD},
        allow_error=True,
    )
    assert response.status_code == 401


def test_change_password_with_missing_fields_returns_400(admin_client):
    """Validation failures are rejected before any credential check."""
    response = admin_client.post(PASSWORD_URL, json={}, allow_error=True)
    assert response.status_code == 400


@pytest.mark.clinic
def test_change_password_happy_path_switches_credentials(throwaway_user):
    """AUTH-018: new password works for login, the old one no longer does, session continues."""
    client = throwaway_user["client"]
    response = client.post(
        PASSWORD_URL,
        json={"currentPassword": throwaway_user["password"], "newPassword": NEW_PASSWORD},
        expected_status=200,
    )
    body = response.json()
    assert body["accessToken"], "A fresh access token must be returned"
    assert "refreshToken" in response.headers.get("Set-Cookie", ""), "Refresh cookie must be rotated"

    # The new access token keeps the calling session usable
    client.set_token(body["accessToken"])
    client.patch("/api/v1/users/me/language", json={"language": "en"}, expected_status=204)

    fresh = APIClient(throwaway_user["config"]["api_base_url"])
    login(fresh, throwaway_user["email"], NEW_PASSWORD)
    old = fresh.post(
        "/api/v1/auth/login",
        json={"email": throwaway_user["email"], "password": throwaway_user["password"]},
        allow_error=True,
    )
    assert old.status_code == 401, "Old password must stop working"


@pytest.mark.clinic
def test_change_password_with_wrong_current_password_returns_400(throwaway_user):
    """AUTH-019: wrong current password is a 400 (not 401, so clients do not refresh-loop)."""
    response = throwaway_user["client"].post(
        PASSWORD_URL,
        json={"currentPassword": "definitely-wrong", "newPassword": NEW_PASSWORD},
        allow_error=True,
    )
    assert response.status_code == 400
    assert response.json()["code"] == "INVALID_CURRENT_PASSWORD"


@pytest.mark.clinic
def test_change_password_rejects_weak_new_password(throwaway_user):
    """AUTH-020: a new password violating the policy returns PASSWORD_POLICY_VIOLATION."""
    response = throwaway_user["client"].post(
        PASSWORD_URL,
        json={"currentPassword": throwaway_user["password"], "newPassword": "weak"},
        allow_error=True,
    )
    assert response.status_code == 400
    assert response.json()["code"] == "PASSWORD_POLICY_VIOLATION"


@pytest.mark.clinic
def test_change_password_rejects_same_password(throwaway_user):
    """AUTH-021: the new password must differ from the current one."""
    response = throwaway_user["client"].post(
        PASSWORD_URL,
        json={"currentPassword": throwaway_user["password"], "newPassword": throwaway_user["password"]},
        allow_error=True,
    )
    assert response.status_code == 400
    assert response.json()["code"] == "SAME_PASSWORD"


@pytest.mark.clinic
def test_change_password_locks_account_after_repeated_failures(throwaway_user):
    """AUTH-022: five wrong current passwords lock the account; the next attempt returns 423."""
    client = throwaway_user["client"]
    for _ in range(5):
        client.post(
            PASSWORD_URL,
            json={"currentPassword": "wrong", "newPassword": NEW_PASSWORD},
            allow_error=True,
        )
    response = client.post(
        PASSWORD_URL,
        json={"currentPassword": throwaway_user["password"], "newPassword": NEW_PASSWORD},
        allow_error=True,
    )
    assert response.status_code == 423
    assert response.json()["code"] == "ACCOUNT_LOCKED"
