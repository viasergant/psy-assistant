"""
Leave management integration tests.
Tests leave request submission, approval/rejection workflow, and authorization.
"""
import pytest
from .utils.data_factory import DataFactory


@pytest.fixture
def therapist_for_leave(admin_client, reference_data, created_resources):
    """Create a therapist for leave request testing."""
    specialization = reference_data["specializations"][0]

    payload = DataFactory.therapist_with_account(
        email=DataFactory.unique_email("leave"),
        full_name="Dr. Leave Test",
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


def test_therapist_submits_leave_request(admin_client, therapist_for_leave, created_resources):
    """Test therapist submitting leave request with status PENDING."""
    therapist_id = therapist_for_leave

    leave_payload = DataFactory.leave_request(
        leave_type="ANNUAL",
        request_notes="Family vacation"
    )

    response = admin_client.post(
        f"/api/v1/therapists/{therapist_id}/leave",
        json=leave_payload,
        expected_status=201
    )

    data = response.json()

    assert data["id"], "Leave request should have ID"
    assert data["startDate"] == leave_payload["startDate"]
    assert data["endDate"] == leave_payload["endDate"]
    assert data["leaveType"] == "ANNUAL"
    assert data["requestNotes"] == "Family vacation"
    assert data["status"] == "PENDING", "New leave should have status PENDING"

    created_resources["leaves"].append(data["id"])


def test_admin_approves_leave_request(admin_client, therapist_for_leave, test_config, created_resources):
    """Test admin approving pending leave request."""
    therapist_id = therapist_for_leave

    # Submit leave request
    leave_payload = DataFactory.leave_request(leave_type="SICK")
    submit_response = admin_client.post(
        f"/api/v1/therapists/{therapist_id}/leave",
        json=leave_payload,
        expected_status=201
    )

    leave_id = submit_response.json()["id"]
    created_resources["leaves"].append(leave_id)

    # Approve leave (using admin endpoint)
    approval_payload = {
        "reviewerUserId": "00000000-0000-0000-0000-000000000000",  # Will be set by backend from JWT
        "adminNotes": "Approved for medical reasons"
    }

    approve_response = admin_client.put(
        f"/api/v1/admin/leave/{leave_id}/approve",
        json=approval_payload,
        expected_status=200
    )

    approved_data = approve_response.json()

    assert approved_data["status"] == "APPROVED"
    assert approved_data["adminNotes"] == "Approved for medical reasons"


def test_admin_rejects_leave_request(admin_client, therapist_for_leave, created_resources):
    """Test admin rejecting pending leave request."""
    therapist_id = therapist_for_leave

    # Submit leave request
    leave_payload = DataFactory.leave_request(leave_type="OTHER")
    submit_response = admin_client.post(
        f"/api/v1/therapists/{therapist_id}/leave",
        json=leave_payload,
        expected_status=201
    )

    leave_id = submit_response.json()["id"]
    created_resources["leaves"].append(leave_id)

    # Reject leave
    rejection_payload = {
        "reviewerUserId": "00000000-0000-0000-0000-000000000000",
        "adminNotes": "Insufficient notice period"
    }

    reject_response = admin_client.put(
        f"/api/v1/admin/leave/{leave_id}/reject",
        json=rejection_payload,
        expected_status=200
    )

    rejected_data = reject_response.json()

    assert rejected_data["status"] == "REJECTED"
    assert rejected_data["adminNotes"] == "Insufficient notice period"


def test_get_pending_leave_requests(admin_client, therapist_for_leave, created_resources):
    """Test admin retrieving list of pending leave requests."""
    therapist_id = therapist_for_leave

    # Submit multiple leave requests
    leave1 = admin_client.post(
        f"/api/v1/therapists/{therapist_id}/leave",
        json=DataFactory.leave_request(leave_type="ANNUAL"),
        expected_status=201
    )
    created_resources["leaves"].append(leave1.json()["id"])

    leave2 = admin_client.post(
        f"/api/v1/therapists/{therapist_id}/leave",
        json=DataFactory.leave_request(leave_type="SICK"),
        expected_status=201
    )
    created_resources["leaves"].append(leave2.json()["id"])

    # Get pending leaves
    response = admin_client.get(
        "/api/v1/admin/leave/pending",
        expected_status=200
    )

    data = response.json()

    assert isinstance(data, list), "Should return list of pending leaves"
    assert len(data) >= 2, "Should contain at least the submitted leaves"

    # All should have status PENDING
    for leave in data:
        assert leave["status"] == "PENDING"


def test_get_therapist_leave_history(admin_client, therapist_for_leave, created_resources):
    """Test retrieving all leave entries for specific therapist."""
    therapist_id = therapist_for_leave

    # Submit leave request
    leave_payload = DataFactory.leave_request()
    submit_response = admin_client.post(
        f"/api/v1/therapists/{therapist_id}/leave",
        json=leave_payload,
        expected_status=201
    )
    created_resources["leaves"].append(submit_response.json()["id"])

    # Get therapist's leaves
    response = admin_client.get(
        f"/api/v1/therapists/{therapist_id}/leave",
        expected_status=200
    )

    data = response.json()

    assert isinstance(data, list), "Should return list of leaves"
    assert len(data) >= 1, "Should contain at least one leave entry"


def test_leave_with_invalid_date_range_returns_400(admin_client, therapist_for_leave):
    """Test submitting leave with end date before start date returns 400."""
    therapist_id = therapist_for_leave

    invalid_payload = {
        "startDate": "2026-04-15",
        "endDate": "2026-04-10",  # Before start date
        "leaveType": "ANNUAL"
    }

    response = admin_client.post(
        f"/api/v1/therapists/{therapist_id}/leave",
        json=invalid_payload,
        allow_error=True
    )

    assert response.status_code == 400, "Should return 400 for invalid date range"


def test_leave_with_missing_leave_type_returns_400(admin_client, therapist_for_leave):
    """Test submitting leave without leave type returns 400."""
    therapist_id = therapist_for_leave

    invalid_payload = {
        "startDate": "2026-04-20",
        "endDate": "2026-04-22"
        # Missing leaveType
    }

    response = admin_client.post(
        f"/api/v1/therapists/{therapist_id}/leave",
        json=invalid_payload,
        allow_error=True
    )

    assert response.status_code == 400, "Should return 400 when leave type missing"


def test_approve_nonexistent_leave_returns_404(admin_client):
    """Test approving non-existent leave request returns 404."""
    fake_uuid = "00000000-0000-0000-0000-000000000000"

    approval_payload = {
        "reviewerUserId": "00000000-0000-0000-0000-000000000001",
        "adminNotes": "Test"
    }

    response = admin_client.put(
        f"/api/v1/admin/leave/{fake_uuid}/approve",
        json=approval_payload,
        allow_error=True
    )

    assert response.status_code == 404, "Should return 404 for nonexistent leave"


def test_leave_operations_require_authentication(unauthenticated_client):
    """Test that leave operations require authentication."""
    fake_therapist_id = "00000000-0000-0000-0000-000000000000"

    response = unauthenticated_client.post(
        f"/api/v1/therapists/{fake_therapist_id}/leave",
        json=DataFactory.leave_request(),
        allow_error=True
    )

    assert response.status_code == 401, "Should return 401 without authentication"
