"""
Caseload overview integration tests.
Tests GET /api/v1/caseload and GET /api/v1/caseload/{therapistProfileId}/clients
with authorization, pagination, and data shape validation.
"""
import pytest
from .utils.data_factory import DataFactory


@pytest.fixture
def therapist_for_caseload(admin_client, reference_data, created_resources):
    """Create a therapist used in caseload tests."""
    specialization = reference_data["specializations"][0]

    payload = DataFactory.therapist_with_account(
        email=DataFactory.unique_email("caseload"),
        full_name="Dr. Caseload Test",
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


def test_admin_can_access_caseload(admin_client):
    """Admin with READ_TEAM_WORKLOAD permission receives paginated caseload list."""
    response = admin_client.get(
        "/api/v1/caseload",
        params={"page": 0, "size": 20},
        expected_status=200
    )

    data = response.json()

    assert "content" in data, "Response should have content array"
    assert "totalElements" in data, "Response should have totalElements"
    assert "totalPages" in data, "Response should have totalPages"
    assert isinstance(data["content"], list), "content should be a list"


def test_caseload_row_shape(admin_client, therapist_for_caseload):
    """Each caseload row contains expected fields when data is present."""
    response = admin_client.get(
        "/api/v1/caseload",
        params={"page": 0, "size": 100},
        expected_status=200
    )

    data = response.json()
    rows = data["content"]

    if rows:
        row = rows[0]
        assert "therapistProfileId" in row, "Row must have therapistProfileId"
        assert "therapistName" in row, "Row must have therapistName"
        assert "activeClientCount" in row, "Row must have activeClientCount"
        assert "sessionsThisWeek" in row, "Row must have sessionsThisWeek"
        assert "sessionsThisMonth" in row, "Row must have sessionsThisMonth"


def test_caseload_pagination(admin_client):
    """Page parameter restricts results correctly."""
    response_p0 = admin_client.get(
        "/api/v1/caseload",
        params={"page": 0, "size": 1},
        expected_status=200
    )

    data = response_p0.json()
    assert len(data["content"]) <= 1, "Page of size 1 should have at most 1 row"


def test_unauthenticated_request_returns_401(unauthenticated_client):
    """Unauthenticated request to caseload returns 401."""
    unauthenticated_client.get(
        "/api/v1/caseload",
        expected_status=401
    )


def test_drilldown_returns_client_list(admin_client, therapist_for_caseload):
    """Admin can access drill-down client list for a therapist."""
    response = admin_client.get(
        f"/api/v1/caseload/{therapist_for_caseload}/clients",
        params={"page": 0, "size": 10},
        expected_status=200
    )

    data = response.json()
    assert "content" in data, "Drilldown response should have content"
    assert "totalElements" in data, "Drilldown response should have totalElements"
    assert isinstance(data["content"], list), "content should be a list"


def test_drilldown_client_row_shape(admin_client, therapist_for_caseload):
    """When clients exist, each drilldown row has expected fields."""
    response = admin_client.get(
        f"/api/v1/caseload/{therapist_for_caseload}/clients",
        params={"page": 0, "size": 10},
        expected_status=200
    )

    data = response.json()
    rows = data["content"]

    if rows:
        row = rows[0]
        assert "clientId" in row, "Row must have clientId"
        assert "clientName" in row, "Row must have clientName"
        assert "completedSessionCount" in row, "Row must have completedSessionCount"
        assert "clientStatus" in row, "Row must have clientStatus"


def test_drilldown_unauthenticated_returns_401(unauthenticated_client, api_client, test_config):
    """Unauthenticated drill-down request returns 401."""
    import uuid
    fake_id = str(uuid.uuid4())
    unauthenticated_client.get(
        f"/api/v1/caseload/{fake_id}/clients",
        expected_status=401
    )


def test_caseload_snapshot_date_filter(admin_client):
    """Snapshot date filter is accepted without server error."""
    response = admin_client.get(
        "/api/v1/caseload",
        params={"page": 0, "size": 10, "snapshotDate": "2025-01-01"},
        expected_status=200
    )

    data = response.json()
    assert "content" in data, "Response with snapshotDate filter should return paginated content"
