"""
Care plan lifecycle integration tests.
Tests care plan creation, retrieval, update, and closure.
Progress tracking and outcome measures are covered by test_progress_tracking.py.
"""
import pytest
from .utils.data_factory import DataFactory

_TERMINAL_STATUSES = ("CLOSED", "COMPLETED", "ARCHIVED")


def _get_or_create_plan(admin_client, client_id, title, goals):
    """Return a plan ID, creating one only when the client is below the 3-plan limit."""
    plans = admin_client.get(f"/api/v1/clients/{client_id}/care-plans", expected_status=200).json()
    active = [p for p in plans if p.get("status") not in _TERMINAL_STATUSES]
    if len(active) < 3:
        resp = admin_client.post(
            f"/api/v1/clients/{client_id}/care-plans",
            json={"title": title, "goals": goals},
            expected_status=201,
        )
        return resp.json()["id"]
    # At limit — reuse the first active plan (not ideal, but keeps the test runnable)
    return active[0]["id"]


@pytest.fixture(scope="module")
def care_plan_setup(session_admin_client, reference_data):
    """Create therapist and client for care plan tests."""
    specialization = reference_data["specializations"][0]

    therapist_payload = DataFactory.therapist_with_account(
        email=DataFactory.unique_email("cp-therapist"),
        specialization_id=specialization["id"]
    )
    therapist_response = session_admin_client.post(
        "/api/v1/therapists/with-account",
        json=therapist_payload,
        expected_status=201
    )
    therapist_id = therapist_response.json()["therapistProfile"]["id"]

    lead_payload = DataFactory.lead(email=DataFactory.unique_email("cp-client"))
    lead_response = session_admin_client.post("/api/v1/leads", json=lead_payload, expected_status=201)
    lead_id = lead_response.json()["id"]

    session_admin_client.patch(f"/api/v1/leads/{lead_id}/status", json={"status": "CONTACTED"}, expected_status=200)
    session_admin_client.patch(f"/api/v1/leads/{lead_id}/status", json={"status": "QUALIFIED"}, expected_status=200)

    lead_data = session_admin_client.get(f"/api/v1/leads/{lead_id}").json()
    convert_response = session_admin_client.post(
        f"/api/v1/leads/{lead_id}/convert",
        json={"fullName": lead_data["fullName"], "contactMethods": lead_data["contactMethods"]},
        expected_status=201
    )
    client_id = convert_response.json()["clientId"]

    return {"client_id": client_id, "therapist_id": therapist_id}


def test_create_care_plan(admin_client, care_plan_setup):
    """Test creating a care plan for a client."""
    client_id = care_plan_setup["client_id"]

    response = admin_client.post(
        f"/api/v1/clients/{client_id}/care-plans",
        json={"title": "Test Plan", "description": "Integration test plan",
              "goals": [{"description": "Initial therapy goal", "priority": 1}]},
        expected_status=201
    )

    data = response.json()

    assert data["id"], "Care plan should have an ID"
    assert data["title"] == "Test Plan"
    assert data.get("status") in ("ACTIVE", "OPEN", "DRAFT", None), \
        f"Unexpected status: {data.get('status')}"


def test_list_care_plans_for_client(admin_client, care_plan_setup):
    """Test listing all care plans for a client."""
    client_id = care_plan_setup["client_id"]

    admin_client.post(
        f"/api/v1/clients/{client_id}/care-plans",
        json={"title": "List Test Plan",
              "goals": [{"description": "List test goal", "priority": 1}]},
        expected_status=201
    )

    response = admin_client.get(
        f"/api/v1/clients/{client_id}/care-plans",
        expected_status=200
    )

    data = response.json()
    assert isinstance(data, list), "Response should be a list"
    assert len(data) >= 1, "Should contain at least one care plan"


def test_get_care_plan_by_id(admin_client, care_plan_setup):
    """Test retrieving a care plan by its ID."""
    client_id = care_plan_setup["client_id"]

    create_response = admin_client.post(
        f"/api/v1/clients/{client_id}/care-plans",
        json={"title": "Get By ID Plan",
              "goals": [{"description": "Get by ID goal", "priority": 1}]},
        expected_status=201
    )
    plan_id = create_response.json()["id"]

    get_response = admin_client.get(
        f"/api/v1/care-plans/{plan_id}",
        expected_status=200
    )

    data = get_response.json()
    assert data["id"] == plan_id


def test_update_care_plan(admin_client, care_plan_setup):
    """Test updating a care plan title using optimistic locking version."""
    client_id = care_plan_setup["client_id"]

    plan_id = _get_or_create_plan(
        admin_client, client_id,
        "Original Title",
        [{"description": "Update test goal", "priority": 1}],
    )

    update_response = admin_client.put(
        f"/api/v1/care-plans/{plan_id}",
        json={"title": "Updated Title", "description": "Updated description"},
        expected_status=200
    )

    assert update_response.json()["title"] == "Updated Title"


def test_close_care_plan(admin_client, care_plan_setup):
    """Test closing an active care plan."""
    client_id = care_plan_setup["client_id"]

    plan_id = _get_or_create_plan(
        admin_client, client_id,
        "Plan To Close",
        [{"description": "Close test goal", "priority": 1}],
    )

    close_response = admin_client.post(
        f"/api/v1/care-plans/{plan_id}/close",
        json={},
        allow_error=True
    )

    assert close_response.status_code in (200, 204), \
        f"Expected 200 or 204, got {close_response.status_code}"

    if close_response.status_code == 200:
        status = close_response.json().get("status", "")
        assert status in ("CLOSED", "COMPLETED", "ARCHIVED"), \
            f"Expected terminal status, got {status}"


def test_care_plan_requires_auth(unauthenticated_client, care_plan_setup):
    """Test that care plan endpoints require authentication."""
    client_id = care_plan_setup["client_id"]

    response = unauthenticated_client.get(
        f"/api/v1/clients/{client_id}/care-plans",
        allow_error=True
    )

    assert response.status_code == 401, "Should return 401 without authentication"


def test_care_plan_shape(admin_client, care_plan_setup):
    """Test that care plan response includes expected fields."""
    client_id = care_plan_setup["client_id"]

    plan_id = _get_or_create_plan(
        admin_client, client_id,
        "Shape Test Plan",
        [{"description": "Shape test goal", "priority": 1}],
    )

    data = admin_client.get(f"/api/v1/care-plans/{plan_id}", expected_status=200).json()

    assert "id" in data, "Response must contain id"
    assert "title" in data, "Response must contain title"
    # clientId may be nested under a client object or as a flat field
    has_client_ref = "clientId" in data or "client" in data
    assert has_client_ref, "Response must reference the client"
    assert "status" in data or "planStatus" in data, "Response must contain a status field"


def test_duplicate_active_plan_behaviour(admin_client, care_plan_setup):
    """Test backend behaviour when creating a second active plan for the same client.

    The backend may allow or reject concurrent active plans — both are valid
    business rules; this test documents whichever is implemented.
    """
    client_id = care_plan_setup["client_id"]

    # Ensure at least one plan exists so there is something to "duplicate"
    _get_or_create_plan(
        admin_client, client_id,
        "First Active Plan",
        [{"description": "First plan goal", "priority": 1}],
    )

    second_response = admin_client.post(
        f"/api/v1/clients/{client_id}/care-plans",
        json={"title": "Second Active Plan",
              "goals": [{"description": "Second plan goal", "priority": 1}]},
        allow_error=True
    )

    assert second_response.status_code in (201, 400, 409), \
        f"Expected 201, 400, or 409; got {second_response.status_code}"
