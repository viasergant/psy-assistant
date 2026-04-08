"""
Progress tracking and outcome measure recording integration tests.
Tests goal status updates, progress note creation, outcome measure recording,
threshold flagging, visibility rules, and chart data endpoints.

NOTE: These tests require a running backend with PA-43 (care plans) data.
Fixtures will create the necessary care plan and client data.
"""
import pytest
from datetime import date, timedelta


# ---------------------------------------------------------------------------
# Fixtures
# ---------------------------------------------------------------------------

@pytest.fixture
def care_plan_setup(admin_client, reference_data, created_resources):
    """Create a client, therapist, and active care plan for progress testing."""
    specialization = reference_data["specializations"][0]

    # Create therapist
    from .utils.data_factory import DataFactory
    therapist_payload = DataFactory.therapist_with_account(
        email=DataFactory.unique_email("progress-tracking"),
        full_name="Dr. Progress Test",
        specialization_id=specialization["id"]
    )
    therapist_resp = admin_client.post(
        "/api/v1/therapists/with-account",
        json=therapist_payload,
        expected_status=201
    )
    therapist_id = therapist_resp.json()["therapistProfile"]["id"]
    therapist_user_id = therapist_resp.json()["userId"]
    created_resources["therapists"].append(therapist_id)

    # Create client
    client_payload = {
        "firstName": "Progress",
        "lastName": "TestClient",
        "email": DataFactory.unique_email("progress-client"),
        "phone": "+380501234567"
    }
    client_resp = admin_client.post("/api/v1/clients", json=client_payload, expected_status=201)
    client_id = client_resp.json()["id"]
    created_resources["clients"].append(client_id)

    # Create active care plan
    plan_payload = {
        "title": "Test Progress Plan",
        "description": "Plan for progress tracking integration tests",
        "goals": [
            {
                "description": "Reduce PHQ-9 score below 10",
                "priority": 1,
                "interventions": [],
                "milestones": []
            }
        ]
    }
    plan_resp = admin_client.post(
        f"/api/v1/care-plans/clients/{client_id}",
        json=plan_payload,
        expected_status=201
    )
    plan = plan_resp.json()
    plan_id = plan["id"]
    goal_id = plan["goals"][0]["id"]

    return {
        "therapist_id": therapist_id,
        "therapist_user_id": therapist_user_id,
        "client_id": client_id,
        "plan_id": plan_id,
        "goal_id": goal_id
    }


# ---------------------------------------------------------------------------
# Goal Status Update
# ---------------------------------------------------------------------------

def test_update_goal_status_success(admin_client, care_plan_setup):
    """Therapist can update goal status on an active care plan."""
    plan_id = care_plan_setup["plan_id"]
    goal_id = care_plan_setup["goal_id"]

    response = admin_client.patch(
        f"/api/v1/care-plans/{plan_id}/goals/{goal_id}/status",
        json={"status": "IN_PROGRESS"},
        expected_status=200
    )

    data = response.json()
    assert data["status"] == "IN_PROGRESS", "Goal status should be IN_PROGRESS"


def test_update_goal_status_to_paused(admin_client, care_plan_setup):
    """Therapist can set goal status to PAUSED."""
    plan_id = care_plan_setup["plan_id"]
    goal_id = care_plan_setup["goal_id"]

    response = admin_client.patch(
        f"/api/v1/care-plans/{plan_id}/goals/{goal_id}/status",
        json={"status": "PAUSED"},
        expected_status=200
    )

    assert response.json()["status"] == "PAUSED"


def test_update_goal_status_to_achieved(admin_client, care_plan_setup):
    """Therapist can set goal status to ACHIEVED."""
    plan_id = care_plan_setup["plan_id"]
    goal_id = care_plan_setup["goal_id"]

    response = admin_client.patch(
        f"/api/v1/care-plans/{plan_id}/goals/{goal_id}/status",
        json={"status": "ACHIEVED"},
        expected_status=200
    )

    assert response.json()["status"] == "ACHIEVED"


# ---------------------------------------------------------------------------
# Progress Notes
# ---------------------------------------------------------------------------

def test_add_progress_note_success(admin_client, care_plan_setup):
    """Therapist can add a progress note to a goal."""
    plan_id = care_plan_setup["plan_id"]
    goal_id = care_plan_setup["goal_id"]

    response = admin_client.post(
        f"/api/v1/care-plans/{plan_id}/goals/{goal_id}/progress-notes",
        json={"noteText": "Client reports improved sleep and reduced rumination."},
        expected_status=201
    )

    data = response.json()
    assert data["id"], "Progress note should have an ID"
    assert data["noteText"] == "Client reports improved sleep and reduced rumination."
    assert data["authorName"], "Author name should be populated"
    assert data["createdAt"], "Creation timestamp should be set"


def test_get_progress_notes_returns_created_note(admin_client, care_plan_setup):
    """GET progress-notes returns the previously added note."""
    plan_id = care_plan_setup["plan_id"]
    goal_id = care_plan_setup["goal_id"]

    # Add a note first
    admin_client.post(
        f"/api/v1/care-plans/{plan_id}/goals/{goal_id}/progress-notes",
        json={"noteText": "Note for retrieval test."},
        expected_status=201
    )

    response = admin_client.get(
        f"/api/v1/care-plans/{plan_id}/goals/{goal_id}/progress-notes",
        expected_status=200
    )

    notes = response.json()
    assert len(notes) >= 1, "Should return at least one note"
    note_texts = [n["noteText"] for n in notes]
    assert "Note for retrieval test." in note_texts


def test_progress_note_is_immutable(admin_client, care_plan_setup):
    """Progress notes cannot be edited or deleted — no PUT/DELETE endpoints."""
    plan_id = care_plan_setup["plan_id"]
    goal_id = care_plan_setup["goal_id"]

    create_resp = admin_client.post(
        f"/api/v1/care-plans/{plan_id}/goals/{goal_id}/progress-notes",
        json={"noteText": "Original immutable note."},
        expected_status=201
    )
    note_id = create_resp.json()["id"]

    # PUT should not exist → 404 or 405
    put_resp = admin_client.put(
        f"/api/v1/care-plans/{plan_id}/goals/{goal_id}/progress-notes/{note_id}",
        json={"noteText": "Attempted edit"},
        expected_status=None  # accept any non-2xx
    )
    assert put_resp.status_code in (404, 405), \
        f"PUT on progress note should return 404 or 405, got {put_resp.status_code}"

    # DELETE should not exist → 404 or 405
    del_resp = admin_client.delete(
        f"/api/v1/care-plans/{plan_id}/goals/{goal_id}/progress-notes/{note_id}",
        expected_status=None
    )
    assert del_resp.status_code in (404, 405), \
        f"DELETE on progress note should return 404 or 405, got {del_resp.status_code}"


def test_progress_note_requires_non_empty_text(admin_client, care_plan_setup):
    """Adding a progress note without noteText returns 400."""
    plan_id = care_plan_setup["plan_id"]
    goal_id = care_plan_setup["goal_id"]

    admin_client.post(
        f"/api/v1/care-plans/{plan_id}/goals/{goal_id}/progress-notes",
        json={"noteText": ""},
        expected_status=400
    )


# ---------------------------------------------------------------------------
# Outcome Measure Definitions
# ---------------------------------------------------------------------------

def test_get_outcome_measure_definitions(admin_client):
    """GET /outcome-measure-definitions returns 7 seeded definitions."""
    response = admin_client.get("/api/v1/outcome-measure-definitions", expected_status=200)

    definitions = response.json()
    assert len(definitions) == 7, f"Expected 7 seeded definitions, got {len(definitions)}"

    codes = {d["code"] for d in definitions}
    assert codes == {"PHQ9", "GAD7", "DASS21_DEP", "DASS21_ANX", "DASS21_STR", "WHODAS", "PCL5"}


def test_outcome_definitions_include_alert_threshold(admin_client):
    """Each definition should include alertThreshold, alertLabel, alertSeverity."""
    response = admin_client.get("/api/v1/outcome-measure-definitions", expected_status=200)

    phq9 = next(d for d in response.json() if d["code"] == "PHQ9")
    assert phq9["alertThreshold"] == 15
    assert phq9["alertLabel"] == "Severe Depression Risk"
    assert phq9["alertSeverity"] == "ALERT"


# ---------------------------------------------------------------------------
# Outcome Measure Entry Recording
# ---------------------------------------------------------------------------

def test_record_outcome_measure_no_threshold_breach(admin_client, care_plan_setup):
    """Recording PHQ-9 score of 5 returns thresholdBreached=false."""
    plan_id = care_plan_setup["plan_id"]

    # Get definition ID for PHQ9
    defs = admin_client.get("/api/v1/outcome-measure-definitions", expected_status=200).json()
    phq9_id = next(d["id"] for d in defs if d["code"] == "PHQ9")

    response = admin_client.post(
        f"/api/v1/care-plans/{plan_id}/outcome-measures",
        json={
            "measureDefinitionId": phq9_id,
            "score": 5,
            "assessmentDate": str(date.today() - timedelta(days=1))
        },
        expected_status=201
    )

    data = response.json()
    assert data["score"] == 5
    assert data["thresholdBreached"] is False
    assert data["alertLabel"] is None


def test_record_outcome_measure_threshold_breached(admin_client, care_plan_setup):
    """Recording PHQ-9 score of 18 returns thresholdBreached=true with alertLabel."""
    plan_id = care_plan_setup["plan_id"]

    defs = admin_client.get("/api/v1/outcome-measure-definitions", expected_status=200).json()
    phq9_id = next(d["id"] for d in defs if d["code"] == "PHQ9")

    response = admin_client.post(
        f"/api/v1/care-plans/{plan_id}/outcome-measures",
        json={
            "measureDefinitionId": phq9_id,
            "score": 18,
            "assessmentDate": str(date.today() - timedelta(days=1))
        },
        expected_status=201
    )

    data = response.json()
    assert data["score"] == 18
    assert data["thresholdBreached"] is True
    assert data["alertLabel"] == "Severe Depression Risk"
    assert data["alertSeverity"] == "ALERT"


def test_record_outcome_measure_score_required(admin_client, care_plan_setup):
    """Recording outcome measure without score returns 400."""
    plan_id = care_plan_setup["plan_id"]

    defs = admin_client.get("/api/v1/outcome-measure-definitions", expected_status=200).json()
    phq9_id = next(d["id"] for d in defs if d["code"] == "PHQ9")

    admin_client.post(
        f"/api/v1/care-plans/{plan_id}/outcome-measures",
        json={
            "measureDefinitionId": phq9_id,
            "assessmentDate": str(date.today() - timedelta(days=1))
        },
        expected_status=400
    )


def test_record_outcome_measure_score_out_of_range(admin_client, care_plan_setup):
    """Recording PHQ-9 score of 30 (above max 27) returns 400."""
    plan_id = care_plan_setup["plan_id"]

    defs = admin_client.get("/api/v1/outcome-measure-definitions", expected_status=200).json()
    phq9_id = next(d["id"] for d in defs if d["code"] == "PHQ9")

    admin_client.post(
        f"/api/v1/care-plans/{plan_id}/outcome-measures",
        json={
            "measureDefinitionId": phq9_id,
            "score": 30,
            "assessmentDate": str(date.today() - timedelta(days=1))
        },
        expected_status=400
    )


def test_record_outcome_measure_future_date_rejected(admin_client, care_plan_setup):
    """Recording outcome measure with assessment date in the future returns 400."""
    plan_id = care_plan_setup["plan_id"]

    defs = admin_client.get("/api/v1/outcome-measure-definitions", expected_status=200).json()
    phq9_id = next(d["id"] for d in defs if d["code"] == "PHQ9")

    admin_client.post(
        f"/api/v1/care-plans/{plan_id}/outcome-measures",
        json={
            "measureDefinitionId": phq9_id,
            "score": 10,
            "assessmentDate": str(date.today() + timedelta(days=1))
        },
        expected_status=400
    )


# ---------------------------------------------------------------------------
# Chart Data
# ---------------------------------------------------------------------------

def test_outcome_measure_chart_data_ordered_asc(admin_client, care_plan_setup):
    """Chart data endpoint returns entries ordered by assessmentDate ascending."""
    plan_id = care_plan_setup["plan_id"]

    defs = admin_client.get("/api/v1/outcome-measure-definitions", expected_status=200).json()
    phq9_id = next(d["id"] for d in defs if d["code"] == "PHQ9")

    today = date.today()
    scores_with_dates = [
        (12, today - timedelta(days=30)),
        (8, today - timedelta(days=14)),
        (15, today - timedelta(days=7)),
    ]

    for score, assessment_date in scores_with_dates:
        admin_client.post(
            f"/api/v1/care-plans/{plan_id}/outcome-measures",
            json={
                "measureDefinitionId": phq9_id,
                "score": score,
                "assessmentDate": str(assessment_date)
            },
            expected_status=201
        )

    response = admin_client.get(
        f"/api/v1/care-plans/{plan_id}/outcome-measures/chart-data?measureCode=PHQ9",
        expected_status=200
    )

    data = response.json()
    assert data["measureCode"] == "PHQ9"
    assert data["alertThreshold"] == 15

    series = data["series"]
    assert len(series) >= 3, "Should have at least 3 data points"

    dates = [s["date"] for s in series]
    assert dates == sorted(dates), "Chart data series should be ordered by date ascending"


def test_get_progress_history_returns_combined_timeline(admin_client, care_plan_setup):
    """GET progress-history returns status change events and notes on a combined timeline."""
    plan_id = care_plan_setup["plan_id"]
    goal_id = care_plan_setup["goal_id"]

    # Update status
    admin_client.patch(
        f"/api/v1/care-plans/{plan_id}/goals/{goal_id}/status",
        json={"status": "IN_PROGRESS"},
        expected_status=200
    )

    # Add a progress note
    admin_client.post(
        f"/api/v1/care-plans/{plan_id}/goals/{goal_id}/progress-notes",
        json={"noteText": "Good week, client engaged."},
        expected_status=201
    )

    response = admin_client.get(
        f"/api/v1/care-plans/{plan_id}/goals/{goal_id}/progress-history",
        expected_status=200
    )

    data = response.json()
    assert data["goalId"] == goal_id
    assert "statusHistory" in data
    assert "progressNotes" in data
    assert len(data["progressNotes"]) >= 1
