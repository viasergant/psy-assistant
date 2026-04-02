"""
Session record integration tests.
Tests session creation, retrieval, and filtering.
"""
import pytest
from datetime import datetime, timedelta
from .utils.data_factory import DataFactory


@pytest.fixture
def session_setup(admin_client, reference_data, created_resources):
    """Create therapist, client, and appointment for session tests."""
    specialization = reference_data["specializations"][0]
    session_type = reference_data["session_types"][0]

    # Create therapist
    therapist_payload = DataFactory.therapist_with_account(
        email=DataFactory.unique_email("session-therapist"),
        specialization_id=specialization["id"]
    )
    therapist_response = admin_client.post(
        "/api/v1/therapists/with-account",
        json=therapist_payload,
        expected_status=201
    )
    therapist_id = therapist_response.json()["therapistProfile"]["id"]
    created_resources["therapists"].append(therapist_id)

    # Create client
    lead_payload = DataFactory.lead(email=DataFactory.unique_email("session-client"))
    lead_response = admin_client.post("/api/v1/leads", json=lead_payload, expected_status=201)
    lead_id = lead_response.json()["id"]
    created_resources["leads"].append(lead_id)

    admin_client.patch(f"/api/v1/leads/{lead_id}/status", json={"status": "CONTACTED"}, expected_status=200)
    admin_client.patch(f"/api/v1/leads/{lead_id}/status", json={"status": "QUALIFIED"}, expected_status=200)

    lead_data = admin_client.get(f"/api/v1/leads/{lead_id}").json()
    convert_response = admin_client.post(
        f"/api/v1/leads/{lead_id}/convert",
        json={"fullName": lead_data["fullName"], "contactMethods": lead_data["contactMethods"]},
        expected_status=201
    )
    client_id = convert_response.json()["clientId"]
    created_resources["clients"].append(client_id)

    # Create appointment
    appointment_payload = DataFactory.appointment(
        therapist_profile_id=therapist_id,
        client_id=client_id,
        session_type_id=session_type["id"]
    )

    appointment_response = admin_client.post(
        "/api/v1/appointments",
        json=appointment_payload,
        expected_status=201
    )

    appointment_id = appointment_response.json()["id"]
    created_resources["appointments"].append(appointment_id)

    return {
        "therapist_id": therapist_id,
        "client_id": client_id,
        "appointment_id": appointment_id
    }


def test_start_session_from_appointment(admin_client, session_setup, created_resources):
    """Test starting session record from appointment."""
    setup = session_setup

    session_payload = {
        "appointmentId": setup["appointment_id"]
    }

    response = admin_client.post(
        "/api/sessions/start",
        json=session_payload,
        expected_status=201
    )

    data = response.json()

    assert data["id"], "Session should have ID"
    assert data["appointmentId"] == setup["appointment_id"]
    assert data["therapistId"] == setup["therapist_id"]
    assert data["clientId"] == setup["client_id"]
    assert data["status"] in ["IN_PROGRESS", "SCHEDULED"]

    created_resources["sessions"].append(data["id"])


def test_get_session_by_id(admin_client, session_setup, created_resources):
    """Test retrieving session record by ID."""
    setup = session_setup

    # Start session
    session_payload = {"appointmentId": setup["appointment_id"]}
    start_response = admin_client.post(
        "/api/sessions/start",
        json=session_payload,
        expected_status=201
    )

    session_id = start_response.json()["id"]
    created_resources["sessions"].append(session_id)

    # Retrieve session
    get_response = admin_client.get(
        f"/api/sessions/{session_id}",
        expected_status=200
    )

    data = get_response.json()

    assert data["id"] == session_id
    assert data["appointmentId"] == setup["appointment_id"]


def test_get_session_by_appointment_id(admin_client, session_setup, created_resources):
    """Test retrieving session by appointment ID."""
    setup = session_setup

    # Start session
    session_payload = {"appointmentId": setup["appointment_id"]}
    start_response = admin_client.post(
        "/api/sessions/start",
        json=session_payload,
        expected_status=201
    )

    session_id = start_response.json()["id"]
    created_resources["sessions"].append(session_id)

    # Retrieve by appointment ID
    get_response = admin_client.get(
        f"/api/sessions/by-appointment/{setup['appointment_id']}",
        expected_status=200
    )

    data = get_response.json()
    assert data["id"] == session_id


def test_list_sessions_filtered_by_therapist(admin_client, session_setup, created_resources):
    """Test listing sessions filtered by therapist ID."""
    setup = session_setup

    # Start session
    session_payload = {"appointmentId": setup["appointment_id"]}
    start_response = admin_client.post(
        "/api/sessions/start",
        json=session_payload,
        expected_status=201
    )
    created_resources["sessions"].append(start_response.json()["id"])

    # List sessions for therapist
    response = admin_client.get(
        "/api/sessions",
        params={"therapistId": setup["therapist_id"]},
        expected_status=200
    )

    data = response.json()

    assert isinstance(data, list), "Should return list of sessions"
    assert len(data) >= 1, "Should contain created session"

    # Verify all sessions belong to therapist
    for session in data:
        assert session["therapistId"] == setup["therapist_id"]


def test_list_sessions_filtered_by_date_range(admin_client, session_setup, created_resources):
    """Test listing sessions filtered by date range."""
    setup = session_setup

    # Start session
    session_payload = {"appointmentId": setup["appointment_id"]}
    start_response = admin_client.post(
        "/api/sessions/start",
        json=session_payload,
        expected_status=201
    )
    created_resources["sessions"].append(start_response.json()["id"])

    # List sessions for date range
    today = datetime.now().strftime('%Y-%m-%d')
    next_week = (datetime.now() + timedelta(days=7)).strftime('%Y-%m-%d')

    response = admin_client.get(
        "/api/sessions",
        params={
            "startDate": today,
            "endDate": next_week
        },
        expected_status=200
    )

    data = response.json()
    assert isinstance(data, list), "Should return list"


def test_list_sessions_filtered_by_status(admin_client, session_setup, created_resources):
    """Test listing sessions filtered by status."""
    setup = session_setup

    # Start session
    session_payload = {"appointmentId": setup["appointment_id"]}
    start_response = admin_client.post(
        "/api/sessions/start",
        json=session_payload,
        expected_status=201
    )
    created_resources["sessions"].append(start_response.json()["id"])

    # List sessions by status
    response = admin_client.get(
        "/api/sessions",
        params={"status": "IN_PROGRESS"},
        expected_status=200
    )

    data = response.json()
    assert isinstance(data, list)


def test_session_operations_require_authentication(unauthenticated_client):
    """Test that session operations require authentication."""
    fake_session_id = "00000000-0000-0000-0000-000000000000"

    response = unauthenticated_client.get(
        f"/api/sessions/{fake_session_id}",
        allow_error=True
    )

    assert response.status_code == 401, "Should return 401 without authentication"
