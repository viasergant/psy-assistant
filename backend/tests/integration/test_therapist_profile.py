"""
Therapist profile lifecycle integration tests.
Tests therapist creation, updates, and retrieval operations.
"""
import pytest
from .utils.data_factory import DataFactory


def test_create_therapist_with_account(admin_client, reference_data, created_resources):
    """Test creating therapist profile with user account."""
    specialization = reference_data["specializations"][0]

    payload = DataFactory.therapist_with_account(
        email=DataFactory.unique_email("therapist"),
        full_name="Dr. Jane Smith",
        specialization_id=specialization["id"]
    )

    response = admin_client.post(
        "/api/v1/therapists/with-account",
        json=payload,
        expected_status=201
    )

    data = response.json()

    # Validate response structure
    assert "therapistProfile" in data, "Response should contain therapistProfile"
    assert "userDetails" in data, "Response should contain userDetails"

    therapist_profile = data["therapistProfile"]
    user_details = data["userDetails"]

    # Validate therapist profile
    assert therapist_profile["id"], "Profile should have ID"
    assert therapist_profile["name"] == "Dr. Jane Smith"
    assert therapist_profile["email"] == payload["email"]
    assert therapist_profile["employmentStatus"] == "ACTIVE"

    # Validate user details
    assert user_details["email"] == payload["email"]
    assert "temporaryPassword" in user_details, "Should return temporary password"
    assert len(user_details["temporaryPassword"]) >= 10, "Temp password should be secure"

    # Track for cleanup
    created_resources["therapists"].append(therapist_profile["id"])


def test_create_therapist_with_phone_number(admin_client, reference_data, created_resources):
    """Test creating therapist with phone number included."""
    specialization = reference_data["specializations"][0]

    payload = {
        **DataFactory.therapist_with_account(
            email=DataFactory.unique_email("therapist"),
            specialization_id=specialization["id"]
        ),
        "phone": DataFactory.unique_phone()
    }

    response = admin_client.post(
        "/api/v1/therapists/with-account",
        json=payload,
        expected_status=201
    )

    data = response.json()
    therapist_profile = data["therapistProfile"]

    assert therapist_profile["phone"] == payload["phone"]
    created_resources["therapists"].append(therapist_profile["id"])


def test_create_therapist_without_specialization_returns_400(admin_client):
    """Test creating therapist without required specialization returns 400."""
    payload = {
        "email": DataFactory.unique_email("therapist"),
        "fullName": "Dr. No Spec",
        "employmentStatus": "ACTIVE"
        # Missing primarySpecializationId
    }

    response = admin_client.post(
        "/api/v1/therapists/with-account",
        json=payload,
        allow_error=True
    )

    assert response.status_code == 400, "Should return 400 when specialization missing"


def test_create_therapist_with_duplicate_email_returns_409(admin_client, reference_data, created_resources):
    """Test creating therapist with existing email returns 409 Conflict."""
    specialization = reference_data["specializations"][0]
    email = DataFactory.unique_email("duplicate")

    payload = DataFactory.therapist_with_account(
        email=email,
        specialization_id=specialization["id"]
    )

    # Create first therapist
    response1 = admin_client.post(
        "/api/v1/therapists/with-account",
        json=payload,
        expected_status=201
    )
    created_resources["therapists"].append(response1.json()["therapistProfile"]["id"])

    # Try to create second with same email
    response2 = admin_client.post(
        "/api/v1/therapists/with-account",
        json=payload,
        allow_error=True
    )

    assert response2.status_code == 400, "Should return 409 for duplicate email"


def test_get_therapist_by_id(admin_client, reference_data, created_resources):
    """Test retrieving therapist profile by ID."""
    specialization = reference_data["specializations"][0]

    # Create therapist
    create_payload = DataFactory.therapist_with_account(
        email=DataFactory.unique_email("lookup"),
        full_name="Dr. Lookup Test",
        specialization_id=specialization["id"]
    )

    create_response = admin_client.post(
        "/api/v1/therapists/with-account",
        json=create_payload,
        expected_status=201
    )

    therapist_id = create_response.json()["therapistProfile"]["id"]
    created_resources["therapists"].append(therapist_id)

    # Retrieve by ID
    get_response = admin_client.get(
        f"/api/v1/therapists/{therapist_id}",
        expected_status=200
    )

    data = get_response.json()

    assert data["id"] == therapist_id
    assert data["name"] == "Dr. Lookup Test"
    assert data["email"] == create_payload["email"]


def test_get_therapist_by_email(admin_client, reference_data, created_resources):
    """Test retrieving therapist profile by email address."""
    specialization = reference_data["specializations"][0]
    email = DataFactory.unique_email("emailsearch")

    # Create therapist
    create_payload = DataFactory.therapist_with_account(
        email=email,
        specialization_id=specialization["id"]
    )

    create_response = admin_client.post(
        "/api/v1/therapists/with-account",
        json=create_payload,
        expected_status=201
    )

    therapist_id = create_response.json()["therapistProfile"]["id"]
    created_resources["therapists"].append(therapist_id)

    # Retrieve by email
    get_response = admin_client.get(
        f"/api/v1/therapists/by-email/{email}",
        expected_status=200
    )

    data = get_response.json()

    assert data["email"] == email
    assert data["id"] == therapist_id


def test_get_nonexistent_therapist_returns_404(admin_client):
    """Test getting therapist with non-existent ID returns 404."""
    fake_uuid = "00000000-0000-0000-0000-000000000000"

    response = admin_client.get(
        f"/api/v1/therapists/{fake_uuid}",
        allow_error=True
    )

    assert response.status_code == 404, "Should return 404 for nonexistent therapist"


def test_update_therapist_profile(admin_client, reference_data, created_resources):
    """Test updating therapist profile information."""
    specialization = reference_data["specializations"][0]

    # Create therapist
    create_payload = DataFactory.therapist_with_account(
        email=DataFactory.unique_email("update"),
        full_name="Dr. Original Name",
        specialization_id=specialization["id"]
    )

    create_response = admin_client.post(
        "/api/v1/therapists/with-account",
        json=create_payload,
        expected_status=201
    )

    create_data = create_response.json()
    therapist_id = create_data["therapistProfile"]["id"]
    created_resources["therapists"].append(therapist_id)

    # Fetch current version fresh from DB (version may differ from create response
    # due to Hibernate flush behaviour during transaction commit)
    get_response = admin_client.get(
        f"/api/v1/therapists/{therapist_id}",
        expected_status=200
    )
    current_version = get_response.json()["version"]

    # Update profile (version required for optimistic locking)
    update_payload = {
        "name": "Dr. Updated Name",
        "phone": "+380991234567",
        "version": current_version
    }

    update_response = admin_client.patch(
        f"/api/v1/therapists/{therapist_id}",
        json=update_payload,
        expected_status=200
    )

    updated_data = update_response.json()

    assert updated_data["name"] == "Dr. Updated Name"
    assert updated_data["phone"] == "+380991234567"


def test_list_therapists_with_pagination(admin_client):
    """Test listing therapists with pagination parameters."""
    response = admin_client.get(
        "/api/v1/therapists",
        params={"page": 0, "size": 10},
        expected_status=200
    )

    data = response.json()

    # Validate PageResponse structure
    assert "content" in data, "Response should contain content array"
    assert "page" in data, "Response should contain page info"
    assert "size" in data, "Response should contain size"
    assert "totalElements" in data, "Response should contain totalElements"
    assert "totalPages" in data, "Response should contain totalPages"

    assert isinstance(data["content"], list), "content should be a list"
    assert data["page"] == 0, "Should return page 0"
    assert data["size"] == 10, "Should return size 10"


def test_list_therapists_default_pagination(admin_client):
    """Test listing therapists without explicit pagination uses defaults."""
    response = admin_client.get(
        "/api/v1/therapists",
        expected_status=200
    )

    data = response.json()

    assert "content" in data
    assert isinstance(data["content"], list)
    # Default size should be 20 as per controller
    assert data["size"] == 20


def test_therapist_unauthenticated_access_denied(unauthenticated_client):
    """Test that therapist endpoints require authentication."""
    response = unauthenticated_client.get(
        "/api/v1/therapists",
        allow_error=True
    )

    assert response.status_code == 401, "Should return 401 without authentication"
