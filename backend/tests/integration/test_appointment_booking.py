"""
Appointment booking and management integration tests.
Tests appointment creation, conflicts, rescheduling, and cancellation.
"""
import pytest
from datetime import datetime, timedelta
from .utils.data_factory import DataFactory


@pytest.fixture
def appointment_setup(admin_client, reference_data, created_resources):
    """Create therapist and client for appointment tests."""
    specialization = reference_data["specializations"][0]
    session_type = reference_data["session_types"][0]

    # Create therapist
    therapist_payload = DataFactory.therapist_with_account(
        email=DataFactory.unique_email("appt-therapist"),
        specialization_id=specialization["id"]
    )
    therapist_response = admin_client.post(
        "/api/v1/therapists/with-account",
        json=therapist_payload,
        expected_status=201
    )
    therapist_id = therapist_response.json()["therapistProfile"]["id"]
    created_resources["therapists"].append(therapist_id)

    # Set working hours for tomorrow
    tomorrow = datetime.now() + timedelta(days=1)
    tomorrow_day = tomorrow.isoweekday()

    schedule_payload = DataFactory.recurring_schedule(
        day_of_week=tomorrow_day,
        start_time="09:00:00",
        end_time="18:00:00"
    )
    schedule_response = admin_client.post(
        f"/api/v1/therapists/{therapist_id}/schedule/recurring",
        json=schedule_payload,
        expected_status=201
    )
    created_resources["schedules"].append(schedule_response.json()["id"])

    # Create client
    lead_payload = DataFactory.lead(email=DataFactory.unique_email("appt-client"))
    lead_response = admin_client.post("/api/v1/leads", json=lead_payload, expected_status=201)
    lead_id = lead_response.json()["id"]
    created_resources["leads"].append(lead_id)

    admin_client.patch(f"/api/v1/leads/{lead_id}/status", json={"status": "CONTACTED"}, expected_status=200)
    admin_client.patch(f"/api/v1/leads/{lead_id}/status", json={"status": "QUALIFIED"}, expected_status=200)

    lead_data = admin_client.get(f"/api/v1/leads/{lead_id}").json()
    convert_response = admin_client.post(
        f"/api/v1/leads/{lead_id}/convert",
        json={"fullName": lead_data["fullName"], "contactMethods": lead_data["contactMethods"]},
        expected_status=200
    )
    client_id = convert_response.json()["clientId"]
    created_resources["clients"].append(client_id)

    return {
        "therapist_id": therapist_id,
        "client_id": client_id,
        "session_type_id": session_type["id"]
    }


def test_create_appointment(admin_client, appointment_setup, created_resources):
    """Test creating new appointment."""
    setup = appointment_setup

    appointment_payload = DataFactory.appointment(
        therapist_profile_id=setup["therapist_id"],
        client_id=setup["client_id"],
        session_type_id=setup["session_type_id"],
        notes="Initial consultation"
    )

    response = admin_client.post(
        "/api/v1/appointments",
        json=appointment_payload,
        expected_status=201
    )

    data = response.json()

    assert data["id"], "Appointment should have ID"
    assert data["therapistProfileId"] == setup["therapist_id"]
    assert data["clientId"] == setup["client_id"]
    assert data["sessionTypeId"] == setup["session_type_id"]
    assert data["status"] in ["SCHEDULED", "CONFIRMED"]

    created_resources["appointments"].append(data["id"])


def test_check_appointment_conflicts(admin_client, appointment_setup, created_resources):
    """Test conflict detection for overlapping appointments."""
    setup = appointment_setup

    # Create first appointment
    appointment_time = (datetime.now() + timedelta(days=1)).replace(hour=14, minute=0, second=0).isoformat()

    appointment1 = DataFactory.appointment(
        therapist_profile_id=setup["therapist_id"],
        client_id=setup["client_id"],
        session_type_id=setup["session_type_id"],
        start_time=appointment_time,
        duration_minutes=60
    )

    response1 = admin_client.post(
        "/api/v1/appointments",
        json=appointment1,
        expected_status=201
    )
    created_resources["appointments"].append(response1.json()["id"])

    # Check for conflicts at same time
    conflict_check = {
        "therapistProfileId": setup["therapist_id"],
        "startTime": appointment_time,
        "durationMinutes": 60,
        "excludeAppointmentId": None
    }

    check_response = admin_client.post(
        "/api/v1/appointments/check-conflicts",
        json=conflict_check,
        expected_status=200
    )

    data = check_response.json()
    assert data["hasConflicts"] is True, "Should detect conflict"


def test_reschedule_appointment(admin_client, appointment_setup, created_resources):
    """Test rescheduling existing appointment."""
    setup = appointment_setup

    # Create appointment
    appointment_payload = DataFactory.appointment(
        therapist_profile_id=setup["therapist_id"],
        client_id=setup["client_id"],
        session_type_id=setup["session_type_id"]
    )

    create_response = admin_client.post(
        "/api/v1/appointments",
        json=appointment_payload,
        expected_status=201
    )

    appointment_id = create_response.json()["id"]
    created_resources["appointments"].append(appointment_id)

    # Reschedule to different time
    new_time = (datetime.now() + timedelta(days=2)).replace(hour=10, minute=0, second=0).isoformat()

    reschedule_payload = {
        "newStartTime": new_time,
        "reason": "Client requested different time"
    }

    reschedule_response = admin_client.put(
        f"/api/v1/appointments/{appointment_id}/reschedule",
        json=reschedule_payload,
        expected_status=200
    )

    updated_data = reschedule_response.json()
    assert updated_data["startTime"] == new_time


def test_cancel_appointment(admin_client, appointment_setup, created_resources):
    """Test cancelling appointment with reason."""
    setup = appointment_setup

    # Create appointment
    appointment_payload = DataFactory.appointment(
        therapist_profile_id=setup["therapist_id"],
        client_id=setup["client_id"],
        session_type_id=setup["session_type_id"]
    )

    create_response = admin_client.post(
        "/api/v1/appointments",
        json=appointment_payload,
        expected_status=201
    )

    appointment_id = create_response.json()["id"]
    created_resources["appointments"].append(appointment_id)

    # Cancel appointment
    cancel_payload = {
        "reason": "Client no longer needs appointment",
        "cancellationType": "CLIENT_CANCELLED"
    }

    cancel_response = admin_client.put(
        f"/api/v1/appointments/{appointment_id}/cancel",
        json=cancel_payload,
        expected_status=200
    )

    cancelled_data = cancel_response.json()
    assert cancelled_data["status"] == "CANCELLED"


def test_get_therapist_appointments_by_date_range(admin_client, appointment_setup, created_resources):
    """Test retrieving appointments for therapist within date range."""
    setup = appointment_setup

    # Create appointment
    appointment_payload = DataFactory.appointment(
        therapist_profile_id=setup["therapist_id"],
        client_id=setup["client_id"],
        session_type_id=setup["session_type_id"]
    )

    create_response = admin_client.post(
        "/api/v1/appointments",
        json=appointment_payload,
        expected_status=201
    )
    created_resources["appointments"].append(create_response.json()["id"])

    # Get appointments for date range
    start_date = (datetime.now() + timedelta(days=1)).strftime('%Y-%m-%d')
    end_date = (datetime.now() + timedelta(days=7)).strftime('%Y-%m-%d')

    response = admin_client.get(
        f"/api/v1/appointments/therapist/{setup['therapist_id']}",
        params={"startDate": start_date, "endDate": end_date},
        expected_status=200
    )

    data = response.json()
    assert isinstance(data, list), "Should return list of appointments"
    assert len(data) >= 1, "Should contain created appointment"


def test_update_appointment_status(admin_client, appointment_setup, created_resources):
    """Test updating appointment status."""
    setup = appointment_setup

    # Create appointment
    appointment_payload = DataFactory.appointment(
        therapist_profile_id=setup["therapist_id"],
        client_id=setup["client_id"],
        session_type_id=setup["session_type_id"]
    )

    create_response = admin_client.post(
        "/api/v1/appointments",
        json=appointment_payload,
        expected_status=201
    )

    appointment_id = create_response.json()["id"]
    created_resources["appointments"].append(appointment_id)

    # Update status
    update_payload = {"status": "CONFIRMED"}

    update_response = admin_client.patch(
        f"/api/v1/appointments/{appointment_id}/status",
        json=update_payload,
        expected_status=200
    )

    updated_data = update_response.json()
    assert updated_data["status"] == "CONFIRMED"
