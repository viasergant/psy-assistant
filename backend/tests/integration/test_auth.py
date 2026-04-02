"""
Authentication flow integration tests.
Tests JWT login, token refresh, and access control.
"""
import pytest
import requests
from .utils.auth_helper import APIClient, login, refresh_token


def test_login_with_valid_admin_credentials(unauthenticated_client, test_config):
    """Test successful admin login returns access token (refresh token is in HttpOnly cookie)."""
    response = unauthenticated_client.post(
        "/api/v1/auth/login",
        json={
            "email": test_config["admin_email"],
            "password": test_config["admin_password"]
        },
        expected_status=200
    )

    data = response.json()

    assert "accessToken" in data, "Response should contain accessToken"
    assert "accessTokenExpiresAt" in data, "Response should contain accessTokenExpiresAt"
    assert "tokenType" in data, "Response should contain tokenType"
    assert isinstance(data["accessToken"], str), "accessToken should be a string"
    assert len(data["accessToken"]) > 20, "accessToken should be a valid JWT"
    assert data["tokenType"] == "Bearer", "tokenType should be Bearer"
    
    # Note: refreshToken is delivered via HttpOnly cookie, not in JSON response


def test_login_with_invalid_credentials_returns_401(unauthenticated_client):
    """Test login with wrong password returns 401 Unauthorized."""
    response = unauthenticated_client.post(
        "/api/v1/auth/login",
        json={
            "email": "admin@psyassistant.com",
            "password": "WrongPassword123!"
        },
        allow_error=True
    )

    assert response.status_code == 401, "Should return 401 for invalid credentials"


def test_login_with_nonexistent_user_returns_401(unauthenticated_client):
    """Test login with non-existent email returns 401."""
    response = unauthenticated_client.post(
        "/api/v1/auth/login",
        json={
            "email": "nonexistent@example.com",
            "password": "SomePassword123!"
        },
        allow_error=True
    )

    assert response.status_code == 401, "Should return 401 for non-existent user"


def test_login_with_missing_email_returns_400(unauthenticated_client, test_config):
    """Test login without email returns 400 Bad Request."""
    response = unauthenticated_client.post(
        "/api/v1/auth/login",
        json={
            "password": test_config["admin_password"]
        },
        allow_error=True
    )

    assert response.status_code == 400, "Should return 400 when email is missing"


def test_login_with_missing_password_returns_400(unauthenticated_client, test_config):
    """Test login without password returns 400 Bad Request."""
    response = unauthenticated_client.post(
        "/api/v1/auth/login",
        json={
            "email": test_config["admin_email"]
        },
        allow_error=True
    )

    assert response.status_code == 400, "Should return 400 when password is missing"


def test_access_protected_endpoint_without_token_returns_401(unauthenticated_client):
    """Test accessing protected endpoint without authentication returns 401."""
    response = unauthenticated_client.get(
        "/api/v1/users/me/language",
        allow_error=True
    )

    assert response.status_code == 401, "Should return 401 when no token provided"


def test_access_protected_endpoint_with_valid_token_succeeds(admin_client):
    """Test accessing protected endpoint with valid JWT succeeds."""
    # Try to get specializations (requires authentication)
    response = admin_client.get(
        "/api/v1/specializations",
        allow_error=True
    )

    assert response.status_code == 200, "Should succeed with valid token"


def test_access_with_malformed_token_returns_401(unauthenticated_client):
    """Test accessing protected endpoint with malformed JWT returns 401."""
    unauthenticated_client.set_token("not.a.valid.jwt.token")

    response = unauthenticated_client.get(
        "/api/v1/specializations",
        allow_error=True
    )

    assert response.status_code == 401, "Should return 401 with malformed token"


@pytest.mark.skip(reason="Token refresh endpoint needs to be identified from backend")
def test_token_refresh_with_valid_refresh_token(unauthenticated_client, test_config):
    """Test token refresh flow with valid refresh token."""
    # First login to get refresh token
    login_response = unauthenticated_client.post(
        "/api/v1/auth/login",
        json={
            "email": test_config["admin_email"],
            "password": test_config["admin_password"]
        },
        expected_status=200
    )

    refresh_token_value = login_response.json()["refreshToken"]

    # Refresh the token
    refresh_response = unauthenticated_client.post(
        "/api/v1/auth/refresh",
        json={
            "refreshToken": refresh_token_value
        },
        expected_status=200
    )

    data = refresh_response.json()

    assert "accessToken" in data, "Refresh should return new accessToken"
    assert isinstance(data["accessToken"], str), "New accessToken should be string"
    assert len(data["accessToken"]) > 20, "New accessToken should be valid JWT"


def test_login_helper_function(unauthenticated_client, test_config):
    """Test login helper utility function."""
    token = login(
        unauthenticated_client,
        test_config["admin_email"],
        test_config["admin_password"]
    )

    assert isinstance(token, str), "login() should return token string"
    assert len(token) > 20, "Token should be valid JWT"

    # Verify token was set in client
    assert unauthenticated_client.token == token, "Token should be set in client"


def test_role_based_access_admin_only_endpoint(admin_client):
    """Test that admin token can access admin-only endpoints."""
    # Admin should be able to list therapists
    response = admin_client.get(
        "/api/v1/therapists",
        allow_error=True
    )

    assert response.status_code == 200, "Admin should access therapist list"
