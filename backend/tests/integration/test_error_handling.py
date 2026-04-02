"""
Error handling and edge case integration tests.
Tests API robustness, validation, and error responses.
"""
import pytest
from .utils.data_factory import DataFactory


def test_invalid_uuid_format_returns_400(admin_client):
    """Test that endpoints reject malformed UUIDs with 400."""
    invalid_uuid = "not-a-valid-uuid"

    response = admin_client.get(
        f"/api/v1/therapists/{invalid_uuid}",
        allow_error=True
    )

    assert response.status_code in [400, 404], "Should reject invalid UUID format"


def test_nonexistent_resource_returns_404(admin_client):
    """Test that requesting non-existent resources returns 404."""
    fake_uuid = "00000000-0000-0000-0000-000000000099"

    response = admin_client.get(
        f"/api/v1/therapists/{fake_uuid}",
        allow_error=True
    )

    assert response.status_code == 404, "Should return 404 for nonexistent resource"


def test_malformed_json_returns_400(admin_client, reference_data):
    """Test that malformed JSON payload returns 400."""
    # Send invalid JSON by using raw request
    response = admin_client.session.post(
        f"{admin_client.base_url}/api/v1/therapists/with-account",
        data="{ invalid json }",
        headers=admin_client._build_headers(),
        timeout=admin_client.timeout
    )

    assert response.status_code == 400, "Should return 400 for malformed JSON"


def test_missing_required_field_returns_400(admin_client):
    """Test that omitting required fields returns 400 with validation errors."""
    incomplete_payload = {
        "fullName": "Dr. Missing Fields"
        # Missing email, employmentStatus, primarySpecializationId
    }

    response = admin_client.post(
        "/api/v1/therapists/with-account",
        json=incomplete_payload,
        allow_error=True
    )

    assert response.status_code == 400, "Should return 400 when required fields missing"


def test_empty_request_body_returns_400(admin_client):
    """Test that endpoints requiring body reject empty requests."""
    response = admin_client.post(
        "/api/v1/therapists/with-account",
        json={},
        allow_error=True
    )

    assert response.status_code == 400, "Should return 400 for empty request body"


def test_invalid_email_format_returns_400(admin_client, reference_data):
    """Test that invalid email format is rejected."""
    specialization = reference_data["specializations"][0]

    invalid_payload = {
        "email": "not-an-email",  # Invalid format
        "fullName": "Dr. Bad Email",
        "employmentStatus": "ACTIVE",
        "primarySpecializationId": specialization["id"]
    }

    response = admin_client.post(
        "/api/v1/therapists/with-account",
        json=invalid_payload,
        allow_error=True
    )

    assert response.status_code == 400, "Should return 400 for invalid email"


def test_invalid_enum_value_returns_400(admin_client, reference_data, created_resources):
    """Test that invalid enum values are rejected."""
    # Create therapist and client first
    specialization = reference_data["specializations"][0]
    therapist_payload = DataFactory.therapist_with_account(
        email=DataFactory.unique_email("therapist"),
        specialization_id=specialization["id"]
    )
    therapist_response = admin_client.post("/api/v1/therapists/with-account", json=therapist_payload, expected_status=201)
    therapist_id = therapist_response.json()["therapistProfile"]["id"]
    created_resources["therapists"].append(therapist_id)

    # Create client via lead conversion
    lead_payload = DataFactory.lead(email=DataFactory.unique_email("client"))
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

    session_type = reference_data["session_types"][0]

    # Create appointment
    appointment_payload = DataFactory.appointment(
        therapist_profile_id=therapist_id,
        client_id=client_id,
        session_type_id=session_type["id"]
    )
    appointment_response = admin_client.post("/api/v1/appointments", json=appointment_payload, expected_status=201)
    appointment_id = appointment_response.json()["id"]
    created_resources["appointments"].append(appointment_id)

    # Now try to cancel with invalid enum value for cancellationType
    invalid_cancel_payload = {
        "cancellationType": "INVALID_CANCELLATION_TYPE",
        "reason": "Testing invalid enum validation"
    }

    response = admin_client.put(
        f"/api/v1/appointments/{appointment_id}/cancel",
        json=invalid_cancel_payload,
        allow_error=True
    )

    assert response.status_code == 400, "Should return 400 for invalid enum value"


def test_negative_pagination_params_handled_gracefully(admin_client):
    """Test that negative pagination parameters are rejected or handled."""
    response = admin_client.get(
        "/api/v1/therapists",
        params={"page": -1, "size": -10},
        allow_error=True
    )

    # Should either reject (400) or default to valid values (200)
    assert response.status_code in [200, 400]


def test_excessive_page_size_handled_gracefully(admin_client):
    """Test that excessively large page sizes are capped or rejected."""
    response = admin_client.get(
        "/api/v1/therapists",
        params={"page": 0, "size": 10000},
        allow_error=True
    )

    # Should either cap size or reject
    assert response.status_code in [200, 400]


def test_unsupported_http_method_returns_405(admin_client):
    """Test that unsupported HTTP methods return 405 Method Not Allowed."""
    # DELETE not supported on list endpoint
    response = admin_client.delete(
        "/api/v1/therapists",
        allow_error=True
    )

    assert response.status_code == 405, "Should return 405 for unsupported method"


def test_unauthorized_endpoint_access_returns_403(unauthenticated_client, test_config):
    """Test that accessing admin endpoints without admin role returns 403."""
    # Login as non-admin user (if available) or test with therapist token
    # For now, test that unauthenticated returns 401
    response = unauthenticated_client.get(
        "/api/v1/admin/leave/pending",
        allow_error=True
    )

    # Will return 401 since no token; with therapist token would be 403
    assert response.status_code in [401, 403]


def test_sql_injection_attempt_handled_safely(admin_client):
    """Test that SQL injection attempts are safely rejected."""
    sql_injection = "'; DROP TABLE therapists; --"

    # Try in email field
    payload = {
        "email": sql_injection,
        "fullName": "SQL Injection Test",
        "employmentStatus": "ACTIVE",
        "primarySpecializationId": "00000000-0000-0000-0000-000000000000"
    }

    response = admin_client.post(
        "/api/v1/therapists/with-account",
        json=payload,
        allow_error=True
    )

    # Should fail validation (400) or not execute SQL (500 would be bad)
    assert response.status_code in [400, 404], "Should safely handle SQL injection"


def test_xss_attempt_sanitized(admin_client, reference_data):
    """Test that XSS payloads in fields are sanitized or rejected."""
    specialization = reference_data["specializations"][0]

    xss_payload = "<script>alert('xss')</script>"

    payload = DataFactory.therapist_with_account(
        email=DataFactory.unique_email("xss"),
        full_name=xss_payload,  # XSS in name field
        specialization_id=specialization["id"]
    )

    response = admin_client.post(
        "/api/v1/therapists/with-account",
        json=payload,
        allow_error=True
    )

    # Should either reject or accept and sanitize
    # If accepted, verify sanitization on retrieval
    if response.status_code == 201:
        therapist_id = response.json()["therapistProfile"]["id"]
        get_response = admin_client.get(f"/api/v1/therapists/{therapist_id}")

        # Script tags should be escaped or removed
        name = get_response.json()["name"]
        assert "<script>" not in name, "XSS payload should be sanitized"


def test_concurrent_appointment_booking_conflict_detection(admin_client, reference_data, created_resources):
    """Test that concurrent booking attempts are properly handled with conflict detection."""
    specialization = reference_data["specializations"][0]
    session_type = reference_data["session_types"][0]

    # Create therapist
    therapist_payload = DataFactory.therapist_with_account(
        email=DataFactory.unique_email("concurrent"),
        specialization_id=specialization["id"]
    )
    therapist_response = admin_client.post(
        "/api/v1/therapists/with-account",
        json=therapist_payload,
        expected_status=201
    )
    therapist_id = therapist_response.json()["therapistProfile"]["id"]
    created_resources["therapists"].append(therapist_id)

    # Create two clients
    lead1 = admin_client.post("/api/v1/leads", json=DataFactory.lead(email=DataFactory.unique_email("c1")), expected_status=201)
    lead1_id = lead1.json()["id"]
    admin_client.patch(f"/api/v1/leads/{lead1_id}/status", json={"status": "CONTACTED"}, expected_status=200)
    admin_client.patch(f"/api/v1/leads/{lead1_id}/status", json={"status": "QUALIFIED"}, expected_status=200)
    lead1_data = admin_client.get(f"/api/v1/leads/{lead1_id}").json()
    convert1 = admin_client.post(f"/api/v1/leads/{lead1_id}/convert",
                                   json={"fullName": lead1_data["fullName"], "contactMethods": lead1_data["contactMethods"]},
                                   expected_status=201)
    client1_id = convert1.json()["clientId"]
    created_resources["clients"].append(client1_id)

    lead2 = admin_client.post("/api/v1/leads", json=DataFactory.lead(email=DataFactory.unique_email("c2")), expected_status=201)
    lead2_id = lead2.json()["id"]
    admin_client.patch(f"/api/v1/leads/{lead2_id}/status", json={"status": "CONTACTED"}, expected_status=200)
    admin_client.patch(f"/api/v1/leads/{lead2_id}/status", json={"status": "QUALIFIED"}, expected_status=200)
    lead2_data = admin_client.get(f"/api/v1/leads/{lead2_id}").json()
    convert2 = admin_client.post(f"/api/v1/leads/{lead2_id}/convert",
                                   json={"fullName": lead2_data["fullName"], "contactMethods": lead2_data["contactMethods"]},
                                   expected_status=201)
    client2_id = convert2.json()["clientId"]
    created_resources["clients"].append(client2_id)

    # Try to book overlapping appointments
    from datetime import datetime, timedelta, timezone
    appointment_time = (datetime.now(timezone.utc) + timedelta(days=1)).replace(hour=14, minute=0, second=0, microsecond=0).isoformat()

    appointment1 = DataFactory.appointment(
        therapist_profile_id=therapist_id,
        client_id=client1_id,
        session_type_id=session_type["id"],
        start_time=appointment_time
    )

    appointment2 = DataFactory.appointment(
        therapist_profile_id=therapist_id,
        client_id=client2_id,
        session_type_id=session_type["id"],
        start_time=appointment_time  # Same time!
    )

    # First should succeed
    response1 = admin_client.post(
        "/api/v1/appointments",
        json=appointment1,
        expected_status=201
    )
    created_resources["appointments"].append(response1.json()["id"])

    # Second should fail with conflict
    response2 = admin_client.post(
        "/api/v1/appointments",
        json=appointment2,
        allow_error=True
    )

    assert response2.status_code in [400, 409], "Should detect appointment conflict"
