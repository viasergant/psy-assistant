"""
Lead management integration tests.
Tests lead CRUD, status transitions, and archival.
Note: lead-to-client conversion is covered in test_lead_to_client_conversion.py.
"""
import pytest
from .utils.data_factory import DataFactory


def test_list_leads(admin_client):
    """Test listing leads returns a valid response."""
    response = admin_client.get("/api/v1/leads", expected_status=200)
    data = response.json()

    # API may return a bare list or a paginated page object
    if isinstance(data, list):
        assert isinstance(data, list)
    else:
        assert "content" in data, "Paginated response should have 'content' key"
        assert isinstance(data["content"], list)


def test_create_lead(admin_client, created_resources):
    """Test creating a new lead returns the created resource."""
    payload = DataFactory.lead(email=DataFactory.unique_email("create-lead"))

    response = admin_client.post("/api/v1/leads", json=payload, expected_status=201)
    data = response.json()

    assert data["id"], "Created lead should have an ID"
    assert isinstance(data["fullName"], str), "fullName should be a string"
    assert data["fullName"] == payload["fullName"]

    created_resources["leads"].append(data["id"])


def test_get_lead_by_id(admin_client, created_resources):
    """Test retrieving a lead by its ID."""
    payload = DataFactory.lead(email=DataFactory.unique_email("get-lead"))
    create_response = admin_client.post("/api/v1/leads", json=payload, expected_status=201)
    lead_id = create_response.json()["id"]
    created_resources["leads"].append(lead_id)

    response = admin_client.get(f"/api/v1/leads/{lead_id}", expected_status=200)
    data = response.json()

    assert data["id"] == lead_id


def test_update_lead(admin_client, created_resources):
    """Test updating a lead's full name."""
    payload = DataFactory.lead(email=DataFactory.unique_email("update-lead"))
    create_response = admin_client.post("/api/v1/leads", json=payload, expected_status=201)
    lead = create_response.json()
    lead_id = lead["id"]
    created_resources["leads"].append(lead_id)

    update_payload = {
        "fullName": "Updated Lead Name",
        "contactMethods": lead["contactMethods"]
    }

    response = admin_client.put(
        f"/api/v1/leads/{lead_id}",
        json=update_payload,
        expected_status=200
    )
    data = response.json()

    assert data["fullName"] == "Updated Lead Name"


def test_lead_status_transition_to_contacted(admin_client, created_resources):
    """Test transitioning a NEW lead to CONTACTED status."""
    payload = DataFactory.lead(email=DataFactory.unique_email("status-contacted"))
    create_response = admin_client.post("/api/v1/leads", json=payload, expected_status=201)
    lead_id = create_response.json()["id"]
    created_resources["leads"].append(lead_id)

    response = admin_client.patch(
        f"/api/v1/leads/{lead_id}/status",
        json={"status": "CONTACTED"},
        expected_status=200
    )
    data = response.json()

    assert data["status"] == "CONTACTED"


def test_lead_status_transition_to_qualified(admin_client, created_resources):
    """Test transitioning a lead through NEW → CONTACTED → QUALIFIED."""
    payload = DataFactory.lead(email=DataFactory.unique_email("status-qualified"))
    create_response = admin_client.post("/api/v1/leads", json=payload, expected_status=201)
    lead_id = create_response.json()["id"]
    created_resources["leads"].append(lead_id)

    admin_client.patch(
        f"/api/v1/leads/{lead_id}/status",
        json={"status": "CONTACTED"},
        expected_status=200
    )

    response = admin_client.patch(
        f"/api/v1/leads/{lead_id}/status",
        json={"status": "QUALIFIED"},
        expected_status=200
    )
    data = response.json()

    assert data["status"] == "QUALIFIED"


def test_archive_lead(admin_client, created_resources):
    """Test archiving a lead changes its status to ARCHIVED."""
    payload = DataFactory.lead(email=DataFactory.unique_email("archive-lead"))
    create_response = admin_client.post("/api/v1/leads", json=payload, expected_status=201)
    lead_id = create_response.json()["id"]
    created_resources["leads"].append(lead_id)

    response = admin_client.patch(
        f"/api/v1/leads/{lead_id}/archive",
        expected_status=200
    )
    data = response.json()

    assert data.get("status") == "INACTIVE", f"Expected INACTIVE (archived), got: {data.get('status')}"


def test_list_leads_filtered_by_status(admin_client, created_resources):
    """Test filtering lead list by status returns only matching leads."""
    payload = DataFactory.lead(email=DataFactory.unique_email("filter-lead"))
    create_response = admin_client.post("/api/v1/leads", json=payload, expected_status=201)
    lead_id = create_response.json()["id"]
    created_resources["leads"].append(lead_id)

    admin_client.patch(
        f"/api/v1/leads/{lead_id}/status",
        json={"status": "CONTACTED"},
        expected_status=200
    )

    response = admin_client.get(
        "/api/v1/leads",
        params={"status": "CONTACTED"},
        expected_status=200
    )
    data = response.json()

    items = data if isinstance(data, list) else data.get("content", [])
    assert len(items) >= 1, "Filtered list should contain at least the CONTACTED lead"
    for item in items:
        assert item["status"] == "CONTACTED", f"All items should be CONTACTED, got: {item['status']}"


def test_create_lead_missing_required_field_returns_400(admin_client):
    """Test creating a lead without required fields returns 400."""
    response = admin_client.post("/api/v1/leads", json={}, allow_error=True)

    assert response.status_code == 400, "Empty body should return 400"


def test_lead_access_requires_authentication(unauthenticated_client):
    """Test that lead endpoints require authentication."""
    response = unauthenticated_client.get("/api/v1/leads", allow_error=True)

    assert response.status_code == 401, "Unauthenticated request should return 401"
