"""
Notification template management integration tests.
Tests template listing, creation, update, and preview.
NOTE: Base path is /api/admin/notification-templates (no /v1/).
"""
import pytest


_TEMPLATE_PAYLOAD = {
    "eventType": "APPOINTMENT_REMINDER",
    "channel": "EMAIL",
    "language": "EN",
    "subject": "Reminder: your appointment is at {{appointmentTime}}",
    "body": "Dear {{clientName}}, your appointment is scheduled at {{appointmentTime}}. Please arrive on time.",
}


def test_list_notification_templates(admin_client):
    """Test listing notification templates returns a list."""
    response = admin_client.get(
        "/api/admin/notification-templates",
        expected_status=200,
    )

    data = response.json()
    assert isinstance(data, list), "Response should be a list of templates"


def test_create_notification_template(admin_client):
    """Test creating a notification template returns the created template."""
    response = admin_client.post(
        "/api/admin/notification-templates",
        json=_TEMPLATE_PAYLOAD,
        expected_status=201,
    )

    data = response.json()
    assert data.get("id"), "Created template should have an ID"
    assert data.get("eventType") == "APPOINTMENT_REMINDER", "eventType should match"
    assert data.get("channel") == "EMAIL", "channel should match"


def test_update_notification_template(admin_client):
    """Test updating a notification template's body."""
    create_response = admin_client.post(
        "/api/admin/notification-templates",
        json=_TEMPLATE_PAYLOAD,
        expected_status=201,
    )
    template_id = create_response.json()["id"]

    updated_body = "Updated body: Dear {{clientName}}, your session is confirmed at {{appointmentTime}}."

    # UpdateTemplateRequest only accepts subject and body — not eventType/channel/language
    update_response = admin_client.put(
        f"/api/admin/notification-templates/{template_id}",
        json={"subject": _TEMPLATE_PAYLOAD["subject"], "body": updated_body},
        expected_status=200,
    )

    data = update_response.json()
    assert data.get("body") == updated_body, "Body should be updated"


def test_preview_notification_template(admin_client):
    """Test previewing a notification template renders subject and body."""
    create_response = admin_client.post(
        "/api/admin/notification-templates",
        json=_TEMPLATE_PAYLOAD,
        expected_status=201,
    )
    template_id = create_response.json()["id"]

    # Activate the template before previewing (may be required)
    admin_client.post(
        f"/api/admin/notification-templates/{template_id}/activate",
        allow_error=True,
    )

    preview_response = admin_client.post(
        f"/api/admin/notification-templates/{template_id}/preview",
        expected_status=200,
    )

    data = preview_response.json()
    # Preview response uses renderedSubject / renderedBody keys
    assert "renderedSubject" in data or "subject" in data, \
        f"Preview response should contain renderedSubject or subject; got keys: {list(data.keys())}"
    assert "renderedBody" in data or "body" in data, \
        f"Preview response should contain renderedBody or body; got keys: {list(data.keys())}"
