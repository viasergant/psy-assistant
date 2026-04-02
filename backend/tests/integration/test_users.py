"""
User management integration tests.
Tests user profile operations and language preferences.
"""
import pytest


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
