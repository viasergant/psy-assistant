"""
Global admin user list and lifecycle (suspend / reactivate / delete) integration tests.
The seeded admin only holds GLOBAL_ADMIN when GLOBAL_ADMIN_USERNAME on the backend
matches its email, so the authorised case accepts either outcome and validates the shape on 200.
"""
URL = "/api/v1/global-admin/users"


def test_global_admin_users_requires_authentication(unauthenticated_client):
    """Anonymous callers receive 401."""
    unauthenticated_client.get(URL, expected_status=401)


def test_global_admin_users_list_or_forbidden(admin_client):
    """GLOBAL_ADMIN gets a page with organisation info; any other admin gets 403."""
    response = admin_client.get(URL, params={"size": 5}, allow_error=True)
    assert response.status_code in (200, 403), response.text
    if response.status_code == 200:
        data = response.json()
        assert {"content", "totalElements", "totalPages", "page", "size"} <= data.keys()
        for user in data["content"]:
            assert {"id", "email", "roles", "active", "orgId", "orgName", "orgType"} <= user.keys()
            assert {"presence", "blockReason", "lastSeenAt"} <= user.keys()
            assert user["presence"] in ("ONLINE", "OFFLINE", "BLOCKED")
            if user["presence"] == "BLOCKED":
                assert user["blockReason"] in ("SUSPENDED", "LOCKED")


UNKNOWN_ID = "00000000-0000-0000-0000-000000000000"


def test_global_admin_user_mutations_require_authentication(unauthenticated_client):
    """Anonymous callers receive 401 on suspend, reactivate and delete."""
    unauthenticated_client.post(f"{URL}/{UNKNOWN_ID}/suspension", expected_status=401)
    unauthenticated_client.delete(f"{URL}/{UNKNOWN_ID}/suspension", expected_status=401)
    unauthenticated_client.delete(f"{URL}/{UNKNOWN_ID}", expected_status=401)


def test_global_admin_suspend_unknown_user_is_404_or_forbidden(admin_client):
    """GLOBAL_ADMIN gets 404 for an unknown id; any other admin gets 403."""
    response = admin_client.post(f"{URL}/{UNKNOWN_ID}/suspension", allow_error=True)
    assert response.status_code in (404, 403), response.text


def test_global_admin_delete_unknown_user_is_404_or_forbidden(admin_client):
    """GLOBAL_ADMIN gets 404 for an unknown id; any other admin gets 403."""
    response = admin_client.delete(f"{URL}/{UNKNOWN_ID}", allow_error=True)
    assert response.status_code in (404, 403), response.text


def test_global_admin_user_stats_requires_authentication(unauthenticated_client):
    """Anonymous callers receive 401 on the stats endpoint."""
    unauthenticated_client.get(f"{URL}/stats", expected_status=401)


def test_global_admin_user_stats_or_forbidden(admin_client):
    """GLOBAL_ADMIN gets consistent counts; any other admin gets 403."""
    response = admin_client.get(f"{URL}/stats", allow_error=True)
    assert response.status_code in (200, 403), response.text
    if response.status_code == 200:
        stats = response.json()
        assert {"total", "online", "blocked"} <= stats.keys()
        assert 0 <= stats["online"] <= stats["total"]
        assert 0 <= stats["blocked"] <= stats["total"]
        assert stats["online"] + stats["blocked"] <= stats["total"]
