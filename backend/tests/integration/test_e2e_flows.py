"""
End-to-end flow tests for complete user journeys.
Tests multi-step workflows across features.
"""
import pytest
from datetime import datetime, timedelta, timezone
from .utils.data_factory import DataFactory


@pytest.mark.e2e
def test_e2e_therapist_onboarding(admin_client, reference_data, created_resources):
    """
    End-to-end: Complete therapist onboarding flow.
    Steps: Create therapist → set working hours → request leave → admin approves → verify availability
    """
    specialization = reference_data["specializations"][0]

    # Step 1: Create therapist with account
    therapist_payload = DataFactory.therapist_with_account(
        email=DataFactory.unique_email("e2e-therapist"),
        full_name="Dr. E2E Onboard",
        specialization_id=specialization["id"]
    )

    therapist_response = admin_client.post(
        "/api/v1/therapists/with-account",
        json=therapist_payload,
        expected_status=201
    )

    therapist_id = therapist_response.json()["therapistProfile"]["id"]
    temp_password = therapist_response.json()["userDetails"]["temporaryPassword"]
    created_resources["therapists"].append(therapist_id)

    assert therapist_id, "Therapist should be created"
    assert temp_password, "Temporary password should be generated"

    # Step 2: Set working hours (Monday-Friday, 9-5)
    for day in range(1, 6):  # Mon-Fri
        schedule_payload = DataFactory.recurring_schedule(
            day_of_week=day,
            start_time="09:00:00",
            end_time="17:00:00"
        )

        schedule_response = admin_client.post(
            f"/api/v1/therapists/{therapist_id}/schedule/recurring",
            json=schedule_payload,
            expected_status=201
        )

        created_resources["schedules"].append(schedule_response.json()["id"])

    # Step 3: Request leave for next week
    next_monday = (datetime.now() + timedelta(days=7)).strftime('%Y-%m-%d')
    next_friday = (datetime.now() + timedelta(days=11)).strftime('%Y-%m-%d')

    leave_payload = DataFactory.leave_request(
        start_date=next_monday,
        end_date=next_friday,
        leave_type="ANNUAL",
        request_notes="Planned vacation"
    )

    leave_response = admin_client.post(
        f"/api/v1/therapists/{therapist_id}/leave",
        json=leave_payload,
        expected_status=201
    )

    leave_id = leave_response.json()["id"]
    created_resources["leaves"].append(leave_id)

    assert leave_response.json()["status"] == "PENDING"

    # Step 4: Admin approves leave
    approval_payload = {
        "adminNotes": "Approved"
    }

    approve_response = admin_client.put(
        f"/api/v1/admin/leave/{leave_id}/approve",
        json=approval_payload,
        expected_status=200
    )

    assert approve_response.json()["status"] == "APPROVED"

    # Step 5: Verify schedule summary includes all components
    schedule_summary = admin_client.get(
        f"/api/v1/therapists/{therapist_id}/schedule",
        expected_status=200
    )

    summary_data = schedule_summary.json()

    assert len(summary_data["recurringSchedule"]) == 5, "Should have Mon-Fri schedules"
    assert len(summary_data["leavePeriods"]) >= 1, "Should include approved leave"


@pytest.mark.e2e
def test_e2e_lead_to_appointment(admin_client, reference_data, created_resources):
    """
    End-to-end: Lead to appointment flow.
    Steps: Create lead → qualify → convert to client → check availability → book appointment
    """
    specialization = reference_data["specializations"][0]
    session_type = reference_data["session_types"][0]

    # Step 1: Create therapist with availability
    therapist_payload = DataFactory.therapist_with_account(
        email=DataFactory.unique_email("e2e-therapist2"),
        specialization_id=specialization["id"]
    )

    therapist_response = admin_client.post(
        "/api/v1/therapists/with-account",
        json=therapist_payload,
        expected_status=201
    )

    therapist_id = therapist_response.json()["therapistProfile"]["id"]
    created_resources["therapists"].append(therapist_id)

    # Set working hours for tomorrow (get tomorrow's day of week)
    tomorrow = datetime.now(timezone.utc) + timedelta(days=1)
    tomorrow_day = tomorrow.isoweekday()  # 1=Monday, 7=Sunday

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

    # Step 2: Create lead
    lead_payload = DataFactory.lead(
        full_name="E2E Test Client",
        email=DataFactory.unique_email("e2e-client"),
        source="website"
    )

    lead_response = admin_client.post(
        "/api/v1/leads",
        json=lead_payload,
        expected_status=201
    )

    lead_id = lead_response.json()["id"]
    created_resources["leads"].append(lead_id)

    # Step 3: Qualify lead
    admin_client.patch(
        f"/api/v1/leads/{lead_id}/status",
        json={"status": "CONTACTED"},
        expected_status=200
    )

    admin_client.patch(
        f"/api/v1/leads/{lead_id}/status",
        json={"status": "QUALIFIED"},
        expected_status=200
    )

    # Step 4: Convert to client
    lead_data = admin_client.get(f"/api/v1/leads/{lead_id}").json()

    conversion_payload = {
        "fullName": lead_data["fullName"],
        "contactMethods": lead_data["contactMethods"]
    }

    convert_response = admin_client.post(
        f"/api/v1/leads/{lead_id}/convert",
        json=conversion_payload,
        expected_status=201
    )

    client_id = convert_response.json()["clientId"]
    created_resources["clients"].append(client_id)

    # Step 5: Check therapist availability
    tomorrow_str = tomorrow.strftime('%Y-%m-%d')

    availability_response = admin_client.get(
        f"/api/v1/therapists/{therapist_id}/availability",
        params={
            "startDate": tomorrow_str,
            "endDate": tomorrow_str
        },
        expected_status=200
    )

    availability = availability_response.json()
    assert len(availability) > 0, "Therapist should have availability"

    # Step 6: Book appointment
    appointment_time = tomorrow.replace(hour=14, minute=0, second=0, microsecond=0)

    appointment_payload = DataFactory.appointment(
        therapist_profile_id=therapist_id,
        client_id=client_id,
        session_type_id=session_type["id"],
        start_time=appointment_time.isoformat(),
        duration_minutes=60
    )

    appointment_response = admin_client.post(
        "/api/v1/appointments",
        json=appointment_payload,
        expected_status=201
    )

    appointment_id = appointment_response.json()["id"]
    created_resources["appointments"].append(appointment_id)

    assert appointment_response.json()["status"] in ["SCHEDULED", "CONFIRMED"]


@pytest.mark.e2e
def test_e2e_appointment_to_session(admin_client, reference_data, created_resources):
    """
    End-to-end: Appointment to session flow.
    Steps: Create appointment → mark in progress → start session → verify linkage
    """
    specialization = reference_data["specializations"][0]
    session_type = reference_data["session_types"][0]

    # Setup: Create therapist and client
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

    # Create client through lead conversion (shortened)
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

    # Step 1: Create appointment
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

    # Step 2: Update status to IN_PROGRESS (if endpoint exists)
    # This may vary based on actual API implementation

    # Step 3: Start session from appointment
    session_payload = {"appointmentId": appointment_id}

    session_response = admin_client.post(
        "/api/sessions/start",
        json=session_payload,
        expected_status=201
    )

    session_id = session_response.json()["id"]
    created_resources["sessions"].append(session_id)

    # Step 4: Verify session linked to appointment
    session_data = admin_client.get(
        f"/api/sessions/{session_id}",
        expected_status=200
    )

    assert session_data.json()["appointmentId"] == appointment_id
    assert session_data.json()["therapistId"] == therapist_id
    assert session_data.json()["clientId"] == client_id
