"""
Therapist schedule management integration tests.
Tests recurring schedules, overrides, and schedule summaries.
"""
import pytest
from .utils.data_factory import DataFactory


@pytest.fixture
def therapist_with_schedule(admin_client, reference_data, created_resources):
    """Create a therapist for schedule testing."""
    specialization = reference_data["specializations"][0]

    payload = DataFactory.therapist_with_account(
        email=DataFactory.unique_email("schedule"),
        full_name="Dr. Schedule Test",
        specialization_id=specialization["id"]
    )

    response = admin_client.post(
        "/api/v1/therapists/with-account",
        json=payload,
        expected_status=201
    )

    therapist_id = response.json()["therapistProfile"]["id"]
    created_resources["therapists"].append(therapist_id)

    return therapist_id


def test_create_recurring_schedule(admin_client, therapist_with_schedule, created_resources):
    """Test creating recurring weekly schedule for therapist."""
    therapist_id = therapist_with_schedule

    schedule_payload = DataFactory.recurring_schedule(
        day_of_week=1,  # Monday
        start_time="09:00:00",
        end_time="17:00:00"
    )

    response = admin_client.post(
        f"/api/v1/therapists/{therapist_id}/schedule/recurring",
        json=schedule_payload,
        expected_status=201
    )

    data = response.json()

    assert data["id"], "Schedule should have ID"
    assert data["dayOfWeek"] == 1
    assert data["startTime"] == "09:00:00"
    assert data["endTime"] == "17:00:00"
    assert data["timezone"] == "Europe/Kiev"

    created_resources["schedules"].append(data["id"])


def test_create_multiple_recurring_schedules(admin_client, therapist_with_schedule, created_resources):
    """Test creating multiple recurring schedules for different days."""
    therapist_id = therapist_with_schedule

    # Monday 9-5
    monday_schedule = DataFactory.recurring_schedule(day_of_week=1)
    response1 = admin_client.post(
        f"/api/v1/therapists/{therapist_id}/schedule/recurring",
        json=monday_schedule,
        expected_status=201
    )
    created_resources["schedules"].append(response1.json()["id"])

    # Friday 10-3
    friday_schedule = DataFactory.recurring_schedule(
        day_of_week=5,
        start_time="10:00:00",
        end_time="15:00:00"
    )
    response2 = admin_client.post(
        f"/api/v1/therapists/{therapist_id}/schedule/recurring",
        json=friday_schedule,
        expected_status=201
    )
    created_resources["schedules"].append(response2.json()["id"])

    # Both should succeed
    assert response1.json()["dayOfWeek"] == 1
    assert response2.json()["dayOfWeek"] == 5


def test_update_recurring_schedule(admin_client, therapist_with_schedule, created_resources):
    """Test updating existing recurring schedule."""
    therapist_id = therapist_with_schedule

    # Create schedule
    create_payload = DataFactory.recurring_schedule(day_of_week=2)
    create_response = admin_client.post(
        f"/api/v1/therapists/{therapist_id}/schedule/recurring",
        json=create_payload,
        expected_status=201
    )

    schedule_id = create_response.json()["id"]
    created_resources["schedules"].append(schedule_id)

    # Update schedule
    update_payload = {
        "dayOfWeek": 2,
        "startTime": "10:00:00",
        "endTime": "18:00:00",
        "timezone": "Europe/Kiev"
    }

    update_response = admin_client.put(
        f"/api/v1/therapists/{therapist_id}/schedule/recurring/{schedule_id}",
        json=update_payload,
        expected_status=200
    )

    updated_data = update_response.json()

    assert updated_data["startTime"] == "10:00:00"
    assert updated_data["endTime"] == "18:00:00"


def test_delete_recurring_schedule(admin_client, therapist_with_schedule):
    """Test deleting recurring schedule."""
    therapist_id = therapist_with_schedule

    # Create schedule
    create_payload = DataFactory.recurring_schedule(day_of_week=3)
    create_response = admin_client.post(
        f"/api/v1/therapists/{therapist_id}/schedule/recurring",
        json=create_payload,
        expected_status=201
    )

    schedule_id = create_response.json()["id"]

    # Delete schedule
    delete_response = admin_client.delete(
        f"/api/v1/therapists/{therapist_id}/schedule/recurring/{schedule_id}",
        expected_status=204
    )

    assert delete_response.status_code == 204


def test_create_schedule_override(admin_client, therapist_with_schedule, created_resources):
    """Test creating date-specific schedule override."""
    therapist_id = therapist_with_schedule

    override_payload = DataFactory.schedule_override(
        is_available=False  # Mark as unavailable
    )

    response = admin_client.post(
        f"/api/v1/therapists/{therapist_id}/schedule/overrides",
        json=override_payload,
        expected_status=201
    )

    data = response.json()

    assert data["id"], "Override should have ID"
    assert data["date"], "Override should have date"
    assert data["isAvailable"] is False
    assert data["timezone"] == "Europe/Kiev"


def test_get_schedule_summary(admin_client, therapist_with_schedule, created_resources):
    """Test retrieving complete schedule summary with all components."""
    therapist_id = therapist_with_schedule

    # Create recurring schedule
    recurring_payload = DataFactory.recurring_schedule(day_of_week=1)
    recurring_response = admin_client.post(
        f"/api/v1/therapists/{therapist_id}/schedule/recurring",
        json=recurring_payload,
        expected_status=201
    )
    created_resources["schedules"].append(recurring_response.json()["id"])

    # Create override
    override_payload = DataFactory.schedule_override(is_available=False)
    admin_client.post(
        f"/api/v1/therapists/{therapist_id}/schedule/overrides",
        json=override_payload,
        expected_status=201
    )

    # Get summary
    summary_response = admin_client.get(
        f"/api/v1/therapists/{therapist_id}/schedule",
        expected_status=200
    )

    data = summary_response.json()

    # Validate structure
    assert "recurringSchedule" in data, "Summary should contain recurring schedules"
    assert "overrides" in data, "Summary should contain overrides"
    assert "leavePeriods" in data, "Summary should contain leave entries"

    assert isinstance(data["recurringSchedule"], list)
    assert isinstance(data["overrides"], list)
    assert isinstance(data["leavePeriods"], list)

    # Should have at least the ones we created
    assert len(data["recurringSchedule"]) >= 1
    assert len(data["overrides"]) >= 1


def test_get_schedule_summary_with_date_range(admin_client, therapist_with_schedule):
    """Test filtering schedule summary by date range."""
    therapist_id = therapist_with_schedule

    response = admin_client.get(
        f"/api/v1/therapists/{therapist_id}/schedule",
        params={
            "startDate": "2026-04-01",
            "endDate": "2026-04-30"
        },
        expected_status=200
    )

    data = response.json()

    assert "recurringSchedule" in data
    assert "overrides" in data
    assert "leavePeriods" in data


def test_create_schedule_with_invalid_time_range_returns_400(admin_client, therapist_with_schedule):
    """Test creating schedule with end time before start time returns 400."""
    therapist_id = therapist_with_schedule

    invalid_payload = {
        "dayOfWeek": 1,
        "startTime": "17:00:00",
        "endTime": "09:00:00",  # Before start time
        "timezone": "Europe/Kiev"
    }

    response = admin_client.post(
        f"/api/v1/therapists/{therapist_id}/schedule/recurring",
        json=invalid_payload,
        allow_error=True
    )

    assert response.status_code == 400, "Should return 400 for invalid time range"


def test_schedule_operations_require_admin_role(unauthenticated_client, therapist_with_schedule):
    """Test that schedule operations require appropriate authorization."""
    therapist_id = therapist_with_schedule

    response = unauthenticated_client.post(
        f"/api/v1/therapists/{therapist_id}/schedule/recurring",
        json=DataFactory.recurring_schedule(),
        allow_error=True
    )

    assert response.status_code == 401, "Should return 401 without authentication"
