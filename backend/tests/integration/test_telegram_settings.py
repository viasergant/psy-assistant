"""
Telegram notification settings integration tests.

Telegram itself is never called: the backend runs with Telegram disabled (the default), so these
tests cover access control, validation and the self-service settings contract.
"""


def test_settings_require_authentication(unauthenticated_client):
    """Telegram settings are not reachable without a token."""
    unauthenticated_client.get("/api/v1/telegram/settings", expected_status=401)


def test_get_settings_returns_defaults_without_chat_details(admin_client):
    """The owner sees defaults and link state, never a chat id."""
    data = admin_client.get("/api/v1/telegram/settings", expected_status=200).json()

    assert data["reminderMinutesBefore"] >= 5
    assert data["includeClientNames"] is False, "client names must be opt-in"
    assert "chatId" not in data and "chatIdHash" not in data


def test_update_settings_roundtrip(admin_client):
    """Preferences persist and are returned on the next read."""
    payload = {
        "timezone": "Europe/Kyiv", "language": "uk", "digestEnabled": True, "digestTime": "08:15",
        "digestDays": 31, "skipEmptyDigest": False, "reminderEnabled": True,
        "reminderMinutesBefore": 45, "includeClientNames": False,
    }
    admin_client.put("/api/v1/telegram/settings", json=payload, expected_status=200)

    data = admin_client.get("/api/v1/telegram/settings", expected_status=200).json()
    assert data["digestTime"] == "08:15"
    assert data["digestDays"] == 31
    assert data["reminderMinutesBefore"] == 45


def test_update_rejects_invalid_values(admin_client):
    """Unknown time zones and out-of-range lead times are rejected."""
    base = {
        "timezone": "Europe/Kyiv", "language": "en", "digestEnabled": True, "digestTime": "07:30",
        "digestDays": 127, "skipEmptyDigest": True, "reminderEnabled": True,
        "reminderMinutesBefore": 60, "includeClientNames": False,
    }
    admin_client.put("/api/v1/telegram/settings", json={**base, "timezone": "Mars/Base"}, expected_status=400)
    admin_client.put("/api/v1/telegram/settings", json={**base, "reminderMinutesBefore": 1}, expected_status=400)
    admin_client.put("/api/v1/telegram/settings", json={**base, "language": "xx"}, expected_status=400)


def test_link_and_test_message_unavailable_when_not_configured(admin_client):
    """Without a bot token the server answers 503 instead of creating a dead link."""
    admin_client.post("/api/v1/telegram/link", expected_status=503)
    admin_client.post("/api/v1/telegram/test", expected_status=503)


def test_webhook_rejects_calls_without_secret(unauthenticated_client):
    """The public webhook never accepts an update without the secret header (404 when polling mode)."""
    response = unauthenticated_client.post(
        "/api/v1/telegram/webhook",
        json={"update_id": 1, "message": {"text": "/start x", "chat": {"id": 1, "type": "private"}}},
        allow_error=True,
    )
    assert response.status_code in (403, 404)
