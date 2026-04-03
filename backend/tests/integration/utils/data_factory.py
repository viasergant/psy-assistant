"""
Test data factory for generating realistic payloads.
Uses Faker library to create random but valid test data.
"""
from faker import Faker
from typing import Dict, Any, List, Optional
import uuid
from datetime import datetime, timedelta, timezone as dt_timezone


# Initialize Faker with multiple locales for realistic test data
fake = Faker(['en_US', 'uk_UA'])


class DataFactory:
    """Factory for generating test data payloads."""

    @staticmethod
    def therapist_profile(
        email: Optional[str] = None,
        full_name: Optional[str] = None,
        phone: Optional[str] = None,
        specialization_id: Optional[str] = None,
        employment_status: str = "ACTIVE"
    ) -> Dict[str, Any]:
        """
        Generate therapist profile creation payload.

        Args:
            email: Therapist email (generated if not provided)
            full_name: Full name (generated if not provided)
            phone: Phone number (generated if not provided)
            specialization_id: UUID of specialization (required for actual requests)
            employment_status: Employment status (ACTIVE or INACTIVE)

        Returns:
            Therapist profile creation request body
        """
        return {
            "email": email or fake.email(),
            "fullName": full_name or fake.name(),
            "phone": phone or fake.phone_number(),
            "employmentStatus": employment_status,
            "primarySpecializationId": specialization_id or str(uuid.uuid4())
        }

    @staticmethod
    def therapist_with_account(
        email: Optional[str] = None,
        full_name: Optional[str] = None,
        specialization_id: Optional[str] = None
    ) -> Dict[str, Any]:
        """
        Generate request to create therapist with user account.

        Args:
            email: Therapist email (generated if not provided)
            full_name: Full name (generated if not provided)
            specialization_id: UUID of specialization

        Returns:
            CreateTherapistWithAccountRequest body
        """
        payload = {
            "email": email or fake.email(),
            "fullName": full_name or fake.name(),
            "employmentStatus": "ACTIVE",
            "primarySpecializationId": specialization_id or str(uuid.uuid4())
        }
        return payload

    @staticmethod
    def lead(
        full_name: Optional[str] = None,
        email: Optional[str] = None,
        phone: Optional[str] = None,
        source: Optional[str] = None,
        notes: Optional[str] = None
    ) -> Dict[str, Any]:
        """
        Generate lead creation payload.

        Args:
            full_name: Lead's full name
            email: Email address
            phone: Phone number
            source: Lead source (e.g., referral, website)
            notes: Additional notes

        Returns:
            Lead creation request body
        """
        contact_methods = []

        if email or (not email and not phone):
            contact_methods.append({
                "type": "EMAIL",
                "value": email or fake.email(),
                "isPrimary": True
            })

        if phone:
            contact_methods.append({
                "type": "PHONE",
                "value": phone,
                "isPrimary": len(contact_methods) == 0
            })

        payload = {
            "fullName": full_name or fake.name(),
            "contactMethods": contact_methods
        }

        if source:
            payload["source"] = source

        if notes:
            payload["notes"] = notes

        return payload

    @staticmethod
    def recurring_schedule(
        day_of_week: int = 1,  # 1=Monday
        start_time: str = "09:00:00",
        end_time: str = "17:00:00",
        timezone: str = "Europe/Kiev"
    ) -> Dict[str, Any]:
        """
        Generate recurring schedule payload.

        Args:
            day_of_week: 1-7 (Monday-Sunday)
            start_time: Start time in HH:MM:SS format
            end_time: End time in HH:MM:SS format
            timezone: IANA timezone identifier

        Returns:
            Recurring schedule request body
        """
        return {
            "dayOfWeek": day_of_week,
            "startTime": start_time,
            "endTime": end_time,
            "timezone": timezone
        }

    @staticmethod
    def schedule_override(
        date: Optional[str] = None,
        start_time: Optional[str] = "10:00:00",
        end_time: Optional[str] = "14:00:00",
        is_available: bool = True
    ) -> Dict[str, Any]:
        """
        Generate schedule override payload.

        Args:
            date: Date in YYYY-MM-DD format (tomorrow if not provided)
            start_time: Start time (None for all-day unavailability)
            end_time: End time (None for all-day unavailability)
            is_available: Whether this override marks availability or unavailability

        Returns:
            Schedule override request body
        """
        override_date = date or (datetime.now() + timedelta(days=1)).strftime('%Y-%m-%d')

        payload = {
            "overrideDate": override_date,
            "isAvailable": is_available
        }

        if start_time:
            payload["startTime"] = start_time
        if end_time:
            payload["endTime"] = end_time

        return payload

    @staticmethod
    def leave_request(
        start_date: Optional[str] = None,
        end_date: Optional[str] = None,
        leave_type: str = "ANNUAL",
        request_notes: Optional[str] = None
    ) -> Dict[str, Any]:
        """
        Generate leave request payload.

        Args:
            start_date: Start date (YYYY-MM-DD), defaults to 7 days from now
            end_date: End date (YYYY-MM-DD), defaults to 9 days from now
            leave_type: ANNUAL, SICK, PUBLIC_HOLIDAY, OTHER
            request_notes: Optional notes

        Returns:
            Leave request submission body
        """
        default_start = (datetime.now() + timedelta(days=7)).strftime('%Y-%m-%d')
        default_end = (datetime.now() + timedelta(days=9)).strftime('%Y-%m-%d')

        payload = {
            "startDate": start_date or default_start,
            "endDate": end_date or default_end,
            "leaveType": leave_type
        }

        if request_notes:
            payload["requestNotes"] = request_notes

        return payload

    @staticmethod
    def appointment(
        therapist_profile_id: str,
        client_id: str,
        session_type_id: str,
        start_time: Optional[str] = None,
        duration_minutes: int = 60,
        timezone: str = "Europe/Kiev",
        notes: Optional[str] = None,
        allow_conflict_override: bool = False
    ) -> Dict[str, Any]:
        """
        Generate appointment creation payload.

        Args:
            therapist_profile_id: UUID of therapist
            client_id: UUID of client
            session_type_id: UUID of session type
            start_time: ISO 8601 datetime (defaults to tomorrow at 14:00)
            duration_minutes: Appointment duration
            timezone: IANA timezone identifier
            notes: Optional appointment notes
            allow_conflict_override: Whether to allow override on conflicts

        Returns:
            Appointment creation request body
        """
        if not start_time:
            tomorrow_2pm = datetime.now(dt_timezone.utc) + timedelta(days=1)
            tomorrow_2pm = tomorrow_2pm.replace(hour=14, minute=0, second=0, microsecond=0)
            start_time = tomorrow_2pm.isoformat()

        payload = {
            "therapistProfileId": therapist_profile_id,
            "clientId": client_id,
            "sessionTypeId": session_type_id,
            "startTime": start_time,
            "durationMinutes": duration_minutes,
            "timezone": timezone,
            "allowConflictOverride": allow_conflict_override
        }

        if notes:
            payload["notes"] = notes

        return payload

    @staticmethod
    def unique_email(prefix: str = "test") -> str:
        """Generate unique email address for test isolation."""
        unique_id = uuid.uuid4().hex[:8]
        return f"{prefix}.{unique_id}@test.psyassistant.com"

    @staticmethod
    def unique_phone() -> str:
        """Generate unique phone number."""
        return f"+3809{fake.random_int(min=10000000, max=99999999)}"
