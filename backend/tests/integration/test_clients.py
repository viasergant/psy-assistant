"""
Client management integration tests.
Tests client retrieval, updates, risk flags, timeline, and packages.
"""
import pytest
from datetime import date, timedelta
from .utils.data_factory import DataFactory


@pytest.fixture(scope="module")
def client_setup(session_admin_client, reference_data):
    """Create a therapist and a client (via lead conversion) for client tests."""
    specialization = reference_data["specializations"][0]

    # Create therapist
    therapist_payload = DataFactory.therapist_with_account(
        email=DataFactory.unique_email("client-test-therapist"),
        specialization_id=specialization["id"]
    )
    therapist_response = session_admin_client.post(
        "/api/v1/therapists/with-account",
        json=therapist_payload,
        expected_status=201
    )
    therapist_id = therapist_response.json()["therapistProfile"]["id"]

    # Create lead and convert to client
    lead_payload = DataFactory.lead(email=DataFactory.unique_email("client-test-lead"))
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

    # Fetch available risk flag types
    rft_response = session_admin_client.get("/api/v1/risk-flag-types", allow_error=True)
    risk_flag_types = rft_response.json() if rft_response.status_code == 200 else []

    return {
        "therapist_id": therapist_id,
        "client_id": client_id,
        "risk_flag_types": risk_flag_types
    }


def test_list_clients(admin_client):
    """Test listing clients returns a valid paginated or list response."""
    response = admin_client.get("/api/v1/clients", expected_status=200)
    data = response.json()

    if isinstance(data, list):
        assert isinstance(data, list)
    else:
        assert "content" in data, "Paginated response should have 'content' key"
        assert isinstance(data["content"], list)


def test_get_client_by_id(admin_client, client_setup):
    """Test retrieving a client by ID returns the correct record."""
    client_id = client_setup["client_id"]

    response = admin_client.get(f"/api/v1/clients/{client_id}", expected_status=200)
    data = response.json()

    assert data["id"] == client_id


def test_update_client(admin_client, client_setup):
    """Test updating a client's full name via PUT."""
    client_id = client_setup["client_id"]

    # Fetch current client to obtain version for optimistic locking
    get_response = admin_client.get(f"/api/v1/clients/{client_id}", expected_status=200)
    current = get_response.json()
    version = current.get("version", 0)

    update_payload = {
        "fullName": "Updated Client Name",
        "version": version
    }

    response = admin_client.put(
        f"/api/v1/clients/{client_id}",
        json=update_payload,
        expected_status=200
    )
    data = response.json()

    assert data["fullName"] == "Updated Client Name"


def test_get_client_timeline(admin_client, client_setup):
    """Test retrieving the client activity timeline."""
    client_id = client_setup["client_id"]

    response = admin_client.get(f"/api/v1/clients/{client_id}/timeline", expected_status=200)
    data = response.json()

    assert isinstance(data, list), "Timeline should be a list (may be empty for a new client)"


def test_add_risk_flag(admin_client, client_setup):
    """Test adding a risk flag to a client."""
    client_id = client_setup["client_id"]
    risk_flag_types = client_setup["risk_flag_types"]

    if not risk_flag_types:
        pytest.skip("No risk flag types available to test with")

    type_id = risk_flag_types[0]["id"]

    review_date = (date.today() + timedelta(days=30)).isoformat()
    response = admin_client.post(
        f"/api/v1/clients/{client_id}/risk-flags",
        json={"flagTypeId": type_id, "clinicalNote": "Test risk flag", "reviewDate": review_date},
        expected_status=201
    )
    data = response.json()

    assert data["id"], "Risk flag should have an ID"
    assert data["status"] == "ACTIVE", f"New flag status should be ACTIVE, got: {data.get('status')}"


def test_list_risk_flags(admin_client, client_setup):
    """Test listing risk flags for a client."""
    client_id = client_setup["client_id"]
    risk_flag_types = client_setup["risk_flag_types"]

    if not risk_flag_types:
        pytest.skip("No risk flag types available to test with")

    type_id = risk_flag_types[0]["id"]
    review_date = (date.today() + timedelta(days=30)).isoformat()
    admin_client.post(
        f"/api/v1/clients/{client_id}/risk-flags",
        json={"flagTypeId": type_id, "clinicalNote": "Flag for list test", "reviewDate": review_date},
        expected_status=201
    )

    response = admin_client.get(f"/api/v1/clients/{client_id}/risk-flags", expected_status=200)
    data = response.json()

    assert isinstance(data, list), "Risk flags should be a list"
    assert len(data) >= 1, "Should contain at least the flag just created"


def test_resolve_risk_flag(admin_client, client_setup):
    """Test resolving an active risk flag."""
    client_id = client_setup["client_id"]
    risk_flag_types = client_setup["risk_flag_types"]

    if not risk_flag_types:
        pytest.skip("No risk flag types available to test with")

    type_id = risk_flag_types[0]["id"]
    review_date = (date.today() + timedelta(days=30)).isoformat()
    add_response = admin_client.post(
        f"/api/v1/clients/{client_id}/risk-flags",
        json={"flagTypeId": type_id, "clinicalNote": "Flag to resolve", "reviewDate": review_date},
        expected_status=201
    )
    flag_id = add_response.json()["id"]

    response = admin_client.patch(
        f"/api/v1/clients/{client_id}/risk-flags/{flag_id}/resolve",
        json={"resolutionNote": "Resolved during testing"},
        expected_status=200
    )
    data = response.json()

    assert data["status"] == "RESOLVED", f"Flag should be RESOLVED, got: {data.get('status')}"


def test_get_client_packages(admin_client, client_setup):
    """Test retrieving packages for a client returns a list."""
    client_id = client_setup["client_id"]

    response = admin_client.get(f"/api/v1/clients/{client_id}/packages", expected_status=200)
    data = response.json()

    assert isinstance(data, list), "Packages should be a list (may be empty)"


def test_client_access_requires_authentication(unauthenticated_client):
    """Test that client endpoints require authentication."""
    response = unauthenticated_client.get("/api/v1/clients", allow_error=True)

    assert response.status_code == 401, "Unauthenticated request should return 401"
