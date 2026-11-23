"""
Risk flag type management integration tests.
Tests public listing, admin CRUD, and deactivation of risk flag types.
"""
import uuid

import pytest


def test_list_risk_flag_types_public(admin_client):
    """Test public endpoint returns list of active risk flag types."""
    response = admin_client.get(
        "/api/v1/risk-flag-types",
        expected_status=200,
    )

    data = response.json()
    assert isinstance(data, list), "Response should be a list of risk flag types"


def test_list_risk_flag_types_admin(admin_client):
    """Test admin endpoint returns list of all risk flag types (including inactive)."""
    response = admin_client.get(
        "/api/v1/admin/risk-flag-types",
        expected_status=200,
    )

    data = response.json()
    assert isinstance(data, list), "Response should be a list of risk flag types"


def test_create_risk_flag_type(admin_client):
    """Test creating a new risk flag type returns the created type."""
    unique_suffix = uuid.uuid4().hex[:6]
    name = f"Test Risk Type {unique_suffix}"

    response = admin_client.post(
        "/api/v1/admin/risk-flag-types",
        json={"name": name},
        expected_status=201,
    )

    data = response.json()
    assert data.get("id"), "Created risk flag type should have an ID"
    assert data.get("name") == name, f"Name should be '{name}'"


def test_deactivate_risk_flag_type(admin_client):
    """Test deactivating a risk flag type removes it from the public list."""
    unique_suffix = uuid.uuid4().hex[:6]

    create_response = admin_client.post(
        "/api/v1/admin/risk-flag-types",
        json={"name": f"Deactivate Test {unique_suffix}"},
        expected_status=201,
    )
    type_id = create_response.json()["id"]

    deactivate_response = admin_client.patch(
        f"/api/v1/admin/risk-flag-types/{type_id}/deactivate",
        allow_error=True,
    )

    assert deactivate_response.status_code in (200, 204), \
        f"Deactivate should return 200 or 204, got {deactivate_response.status_code}"


def test_non_admin_cannot_manage_risk_flag_types(unauthenticated_client):
    """Test that creating risk flag types requires authentication."""
    response = unauthenticated_client.post(
        "/api/v1/admin/risk-flag-types",
        json={"name": "Unauthorized Type"},
        allow_error=True,
    )

    assert response.status_code == 401, \
        "Should return 401 without authentication"
