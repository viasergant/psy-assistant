"""
Global admin user list integration tests.
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
