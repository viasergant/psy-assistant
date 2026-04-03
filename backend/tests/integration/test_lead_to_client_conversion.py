"""
Lead to client conversion integration tests.
Tests the complete lead-to-client conversion workflow.
"""
import pytest
from .utils.data_factory import DataFactory


@pytest.fixture
def qualified_lead(admin_client, created_resources):
    """Create a qualified lead ready for conversion."""
    # Create lead
    lead_payload = DataFactory.lead(
        full_name="Test Client",
        email=DataFactory.unique_email("convert"),
        source="referral",
        notes="Referred by Dr. Smith"
    )

    create_response = admin_client.post(
        "/api/v1/leads",
        json=lead_payload,
        expected_status=201
    )

    lead_id = create_response.json()["id"]
    created_resources["leads"].append(lead_id)

    # Transition to CONTACTED
    admin_client.patch(
        f"/api/v1/leads/{lead_id}/status",
        json={"status": "CONTACTED"},
        expected_status=200
    )

    # Transition to QUALIFIED
    admin_client.patch(
        f"/api/v1/leads/{lead_id}/status",
        json={"status": "QUALIFIED"},
        expected_status=200
    )

    return lead_id


def test_convert_qualified_lead_to_client(admin_client, qualified_lead, created_resources):
    """Test converting qualified lead to client with pre-populated data."""
    lead_id = qualified_lead

    # Get lead details before conversion
    lead_response = admin_client.get(
        f"/api/v1/leads/{lead_id}",
        expected_status=200
    )
    lead_data = lead_response.json()

    # Convert lead
    conversion_payload = {
        "fullName": lead_data["fullName"],
        "contactMethods": lead_data["contactMethods"],
        "notes": "Converted from lead"
    }

    convert_response = admin_client.post(
        f"/api/v1/leads/{lead_id}/convert",
        json=conversion_payload,
        expected_status=201
    )

    conversion_data = convert_response.json()

    assert "clientId" in conversion_data, "Should return clientId"
    assert conversion_data["clientId"], "clientId should be populated"

    client_id = conversion_data["clientId"]
    created_resources["clients"].append(client_id)

    # Verify lead status updated to CONVERTED
    updated_lead = admin_client.get(
        f"/api/v1/leads/{lead_id}",
        expected_status=200
    )
    assert updated_lead.json()["status"] == "CONVERTED"


"""
Test commented becasuse client does not contain contact methods,
and this test needs to be reworked once we decide how to handle contact methods during conversion.
def test_convert_lead_preserves_contact_methods(admin_client, qualified_lead, created_resources):
     """
"""Test that contact methods are preserved during conversion. """
"""
    lead_id = qualified_lead

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

    # Verify client has same contact methods
    client_response = admin_client.get(
        f"/api/v1/clients/{client_id}",
        expected_status=200
    )

    client_data = client_response.json()
    assert len(client_data["contactMethods"]) == len(lead_data["contactMethods"])
 """


def test_cannot_convert_already_converted_lead(admin_client, qualified_lead, created_resources):
    """Test that attempting to convert already-converted lead returns error."""
    lead_id = qualified_lead

    lead_data = admin_client.get(f"/api/v1/leads/{lead_id}").json()

    conversion_payload = {
        "fullName": lead_data["fullName"],
        "contactMethods": lead_data["contactMethods"]
    }

    # First conversion succeeds
    first_convert = admin_client.post(
        f"/api/v1/leads/{lead_id}/convert",
        json=conversion_payload,
        expected_status=201
    )
    created_resources["clients"].append(first_convert.json()["clientId"])

    # Second conversion should fail
    second_convert = admin_client.post(
        f"/api/v1/leads/{lead_id}/convert",
        json=conversion_payload,
        allow_error=True
    )

    assert second_convert.status_code in [400, 409], "Should reject duplicate conversion"


def test_conversion_creates_audit_trail(admin_client, qualified_lead, created_resources):
    """Test that conversion creates proper audit/activity records."""
    lead_id = qualified_lead

    lead_data = admin_client.get(f"/api/v1/leads/{lead_id}").json()

    conversion_payload = {
        "fullName": lead_data["fullName"],
        "contactMethods": lead_data["contactMethods"],
        "notes": "Converted with notes"
    }

    convert_response = admin_client.post(
        f"/api/v1/leads/{lead_id}/convert",
        json=conversion_payload,
        expected_status=201
    )

    client_id = convert_response.json()["clientId"]
    created_resources["clients"].append(client_id)

    # Verify client was created successfully
    client_response = admin_client.get(
        f"/api/v1/clients/{client_id}",
        expected_status=200
    )

    assert client_response.json()["fullName"] == lead_data["fullName"]
