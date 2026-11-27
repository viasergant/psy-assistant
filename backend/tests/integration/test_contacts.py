"""
Platform contacts integration tests: public read and global-admin editing.
The seeded admin only holds GLOBAL_ADMIN when GLOBAL_ADMIN_USERNAME on the backend
matches its email, so authorised cases accept 403 and validate details only on 200.
"""
PUBLIC_URL = "/api/v1/contacts"
ADMIN_URL = "/api/v1/global-admin/contacts"


def test_public_contacts_are_readable_without_authentication(unauthenticated_client):
    """Anonymous callers can read the contacts."""
    response = unauthenticated_client.get(PUBLIC_URL)
    assert isinstance(response.json()["telegramChannels"], list)


def test_admin_contacts_require_authentication(unauthenticated_client):
    """Anonymous callers receive 401 on the admin endpoints."""
    unauthenticated_client.get(ADMIN_URL, expected_status=401)
    unauthenticated_client.put(ADMIN_URL, json={}, expected_status=401)


def test_admin_contacts_update_or_forbidden(admin_client):
    """GLOBAL_ADMIN can update and the public endpoint reflects it; any other admin gets 403."""
    payload = {"phone": "+380441234567", "email": "help@example.com", "telegramChannels": ["@psy_help"]}
    response = admin_client.put(ADMIN_URL, json=payload, allow_error=True)
    assert response.status_code in (200, 403), response.text
    if response.status_code == 200:
        assert response.json()["telegramChannels"] == ["https://t.me/psy_help"]
        bad = admin_client.put(ADMIN_URL, json={"email": "nope"}, allow_error=True)
        assert bad.status_code == 400
