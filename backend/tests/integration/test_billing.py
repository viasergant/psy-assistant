"""
Billing module integration tests.
Tests service catalog, invoices, payments, refunds, package definitions,
package sales, and discount rules.
"""
import pytest
from datetime import datetime, timezone, date
from .utils.data_factory import DataFactory


@pytest.fixture(scope="module")
def billing_setup(session_admin_client, reference_data):
    """Create therapist, client, and a service catalog item for billing tests."""
    specialization = reference_data["specializations"][0]
    session_type = reference_data["session_types"][0]

    # Create therapist
    therapist_payload = DataFactory.therapist_with_account(
        email=DataFactory.unique_email("billing-therapist"),
        specialization_id=specialization["id"]
    )
    therapist_response = session_admin_client.post(
        "/api/v1/therapists/with-account",
        json=therapist_payload,
        expected_status=201
    )
    therapist_id = therapist_response.json()["therapistProfile"]["id"]

    # Create client via lead conversion
    lead_payload = DataFactory.lead(email=DataFactory.unique_email("billing-client"))
    lead_response = session_admin_client.post("/api/v1/leads", json=lead_payload, expected_status=201)
    lead_id = lead_response.json()["id"]

    session_admin_client.patch(f"/api/v1/leads/{lead_id}/status", json={"status": "CONTACTED"}, expected_status=200)
    session_admin_client.patch(f"/api/v1/leads/{lead_id}/status", json={"status": "QUALIFIED"}, expected_status=200)

    lead_data = session_admin_client.get(f"/api/v1/leads/{lead_id}").json()
    convert_response = session_admin_client.post(
        f"/api/v1/leads/{lead_id}/convert",
        json={"fullName": lead_data["fullName"], "contactMethods": lead_data["contactMethods"]},
        expected_status=201
    )
    client_id = convert_response.json()["clientId"]

    # Create service catalog item
    catalog_payload = {
        "name": f"Integration Test Service {DataFactory.unique_email('svc')}",
        "category": "THERAPY",
        "sessionTypeId": session_type["id"],
        "durationMin": 60,
        "defaultPrice": "150.00",
        "effectiveFrom": date.today().isoformat()
    }
    catalog_response = session_admin_client.post(
        "/api/v1/billing/catalog",
        json=catalog_payload,
        expected_status=201
    )
    catalog_item_id = catalog_response.json()["id"]

    return {
        "client_id": client_id,
        "therapist_id": therapist_id,
        "catalog_item_id": catalog_item_id,
        "session_type_id": session_type["id"]
    }


def test_create_service_catalog_item(admin_client, billing_setup):
    """Test creating a new service catalog item."""
    session_type_id = billing_setup["session_type_id"]

    response = admin_client.post(
        "/api/v1/billing/catalog",
        json={
            "name": f"New Service {DataFactory.unique_email('newsvc')}",
            "category": "THERAPY",
            "sessionTypeId": session_type_id,
            "durationMin": 45,
            "defaultPrice": "120.00",
            "effectiveFrom": date.today().isoformat()
        },
        expected_status=201
    )

    data = response.json()
    assert data["id"], "Catalog item should have an ID"
    assert "name" in data, "Response should contain name"


def test_list_service_catalog(admin_client):
    """Test listing service catalog items."""
    response = admin_client.get(
        "/api/v1/billing/catalog",
        expected_status=200
    )

    data = response.json()
    assert isinstance(data, list), "Response should be a list"


def test_create_manual_invoice(admin_client, billing_setup):
    """Test creating a manual invoice for a client."""
    client_id = billing_setup["client_id"]

    response = admin_client.post(
        "/api/v1/invoices/manual",
        json={
            "clientId": client_id,
            "lineItems": [
                {
                    "description": "Individual therapy session",
                    "quantity": "1",
                    "unitPrice": "150.00"
                }
            ]
        },
        expected_status=201
    )

    data = response.json()
    assert data["id"], "Invoice should have an ID"
    # clientId may be flat or nested under a client object
    client_ref = data.get("clientId") or (data.get("client") or {}).get("id")
    assert client_ref == client_id, "Invoice client should match"
    assert "status" in data, "Invoice should have a status"


def test_list_invoices(admin_client):
    """Test listing all invoices."""
    response = admin_client.get(
        "/api/v1/invoices",
        expected_status=200
    )

    data = response.json()
    # Response may be a plain list or a paginated wrapper
    assert isinstance(data, (list, dict)), "Response should be a list or paginated object"
    if isinstance(data, dict):
        assert "content" in data or "items" in data or "data" in data, \
            "Paginated response should have a content/items/data field"


def test_get_invoice_by_id(admin_client, billing_setup):
    """Test retrieving a specific invoice by ID."""
    client_id = billing_setup["client_id"]

    create_response = admin_client.post(
        "/api/v1/invoices/manual",
        json={
            "clientId": client_id,
            "lineItems": [{"description": "Session", "quantity": "1", "unitPrice": "100.00"}]
        },
        expected_status=201
    )
    invoice_id = create_response.json()["id"]

    get_response = admin_client.get(
        f"/api/v1/invoices/{invoice_id}",
        expected_status=200
    )

    assert get_response.json()["id"] == invoice_id


def test_issue_invoice(admin_client, billing_setup):
    """Test issuing a draft invoice."""
    client_id = billing_setup["client_id"]

    create_response = admin_client.post(
        "/api/v1/invoices/manual",
        json={
            "clientId": client_id,
            "lineItems": [{"description": "Session", "quantity": "1", "unitPrice": "150.00"}]
        },
        expected_status=201
    )
    invoice_id = create_response.json()["id"]

    issue_response = admin_client.post(
        f"/api/v1/invoices/{invoice_id}/issue",
        json={},
        allow_error=True
    )

    assert issue_response.status_code in (200, 204), \
        f"Expected 200 or 204, got {issue_response.status_code}: {issue_response.text}"

    if issue_response.status_code == 200:
        assert issue_response.json().get("status") == "ISSUED", \
            f"Expected ISSUED status, got {issue_response.json().get('status')}"


def test_register_payment(admin_client, billing_setup):
    """Test registering a payment against an issued invoice."""
    client_id = billing_setup["client_id"]

    # Create and issue invoice
    create_response = admin_client.post(
        "/api/v1/invoices/manual",
        json={
            "clientId": client_id,
            "lineItems": [{"description": "Session", "quantity": "1", "unitPrice": "150.00"}]
        },
        expected_status=201
    )
    invoice_id = create_response.json()["id"]
    admin_client.post(f"/api/v1/invoices/{invoice_id}/issue", json={}, allow_error=True)

    # Register payment
    payment_response = admin_client.post(
        f"/api/v1/invoices/{invoice_id}/payments",
        json={
            "amount": "150.00",
            "paymentMethod": "BANK_TRANSFER",
            "paymentDate": "2024-01-15"
        },
        expected_status=201
    )

    data = payment_response.json()
    assert data["id"], "Payment should have an ID"
    assert "amount" in data, "Payment should contain amount"


def test_register_refund(admin_client, billing_setup):
    """Test registering a refund against a paid invoice."""
    client_id = billing_setup["client_id"]

    # Create, issue, and pay invoice
    create_response = admin_client.post(
        "/api/v1/invoices/manual",
        json={
            "clientId": client_id,
            "lineItems": [{"description": "Session", "quantity": "1", "unitPrice": "150.00"}]
        },
        expected_status=201
    )
    invoice_id = create_response.json()["id"]
    admin_client.post(f"/api/v1/invoices/{invoice_id}/issue", json={}, allow_error=True)
    payment_response = admin_client.post(
        f"/api/v1/invoices/{invoice_id}/payments",
        json={
            "amount": "150.00",
            "paymentMethod": "BANK_TRANSFER",
            "paymentDate": "2024-01-15"
        },
        allow_error=True
    )

    if payment_response.status_code != 201:
        pytest.skip("Payment registration failed; skipping refund test")

    payment_id = payment_response.json().get("id")

    # Register refund
    refund_payload = {
        "amount": "50.00",
        "reason": "Partial refund for missed portion",
        "refundDate": "2024-01-16"
    }
    if payment_id:
        refund_payload["paymentId"] = payment_id

    refund_response = admin_client.post(
        f"/api/v1/invoices/{invoice_id}/refunds",
        json=refund_payload,
        expected_status=201
    )

    data = refund_response.json()
    assert data["id"], "Refund should have an ID"
    assert "amount" in data, "Refund should contain amount"


def test_create_package_definition(admin_client):
    """Test creating a package definition."""
    response = admin_client.post(
        "/api/v1/billing/package-definitions",
        json={
            "name": f"Test Pack {DataFactory.unique_email('pack')}",
            "serviceType": "INDIVIDUAL_SESSION",
            "sessionQty": 10,
            "price": "1200.00"
        },
        expected_status=201
    )

    data = response.json()
    assert data["id"], "Package definition should have an ID"
    assert data.get("sessionQty") == 10 or data.get("sessionCount") == 10, \
        "Package should reflect session quantity"


def test_sell_package_to_client(admin_client, billing_setup):
    """Test selling a package to a client and verifying it appears on the client."""
    client_id = billing_setup["client_id"]

    # Create package definition
    def_response = admin_client.post(
        "/api/v1/billing/package-definitions",
        json={
            "name": f"Sell Test Pack {DataFactory.unique_email('sellpack')}",
            "serviceType": "INDIVIDUAL_SESSION",
            "sessionQty": 5,
            "price": "600.00"
        },
        expected_status=201
    )
    def_id = def_response.json()["id"]

    # Sell the package
    sell_response = admin_client.post(
        "/api/v1/billing/packages/sell",
        json={
            "definitionId": def_id,
            "clientId": client_id,
            "purchasedAt": datetime.now(timezone.utc).isoformat()
        },
        expected_status=201
    )

    assert sell_response.json()["id"], "Sold package should have an ID"

    # Verify the package appears on the client
    packages_response = admin_client.get(
        f"/api/v1/clients/{client_id}/packages",
        expected_status=200
    )
    packages = packages_response.json()
    assert isinstance(packages, list), "Client packages should be a list"
    assert len(packages) >= 1, "Client should have at least one package after sale"


def test_create_discount_rule(admin_client, billing_setup):
    """Test creating a discount rule scoped to a service catalog item."""
    response = admin_client.post(
        "/api/v1/billing/discounts",
        json={
            "name": f"Test Discount {DataFactory.unique_email('disc')}",
            "type": "PERCENTAGE",
            "value": 10,
            "scope": "SERVICE",
            "serviceCatalogId": billing_setup["catalog_item_id"]
        },
        expected_status=201
    )

    data = response.json()
    assert data["id"], "Discount rule should have an ID"


def test_list_discount_rules(admin_client):
    """Test listing all discount rules."""
    response = admin_client.get(
        "/api/v1/billing/discounts",
        expected_status=200
    )

    data = response.json()
    assert isinstance(data, list), "Discount rules response should be a list"


def test_billing_access_requires_auth(unauthenticated_client):
    """Test that billing endpoints require authentication."""
    response = unauthenticated_client.get(
        "/api/v1/invoices",
        allow_error=True
    )

    assert response.status_code == 401, "Should return 401 without authentication"
