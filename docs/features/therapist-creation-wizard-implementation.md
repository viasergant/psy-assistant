# Therapist Creation Wizard Implementation

**Date:** March 30, 2026  
**Status:** ✅ Implemented

## Overview

Implemented a streamlined therapist creation wizard that automatically redirects from the general user creation dialog when THERAPIST role is selected. Creates both user account (with temporary password) and therapist profile atomically.

## User Flow

1. Admin opens "Create User" dialog
2. Admin fills in email, full name
3. Admin selects "THERAPIST" role
4. System automatically redirects to Therapist Creation Wizard with prefilled data
5. Admin completes additional therapist-specific fields:
   - Phone number (optional)
   - Employment status (required)
   - Primary specialization (required)
6. System creates:
   - User account with THERAPIST role
   - User gets `mustChangePassword=true` flag
   - Secure temporary password generated
   - Therapist profile with basic information
7. Admin sees temporary password in modal (shown once)
8. Admin can copy and share credentials with therapist

## Backend Changes

### New Endpoint

**POST `/api/v1/therapists/with-account`**

Creates user account and therapist profile atomically in a single transaction.

**Request:**
```json
{
  "email": "therapist@example.com",
  "fullName": "Dr. Jane Smith",
  "phone": "+1234567890",
  "employmentStatus": "FULL_TIME",
  "primarySpecializationId": "uuid-here"
}
```

**Response:**
```json
{
  "userDetails": {
    "id": "user-uuid",
    "email": "therapist@example.com",
    "fullName": "Dr. Jane Smith",
    "role": "THERAPIST",
    "temporaryPassword": "SecureTemp123!"
  },
  "therapistProfile": {
    "id": "profile-uuid",
    "email": "therapist@example.com",
    "name": "Dr. Jane Smith",
    "phone": "+1234567890",
    "employmentStatus": "FULL_TIME",
    "active": true,
    "specializations": [
      {
        "id": "spec-uuid",
        "name": "Cognitive Behavioral Therapy"
      }
    ],
    "languages": [],
    "createdAt": "2026-03-30T10:00:00Z"
  }
}
```

### New DTOs

1. **`CreateTherapistWithAccountRequest`**
   - Validation: email (required, valid), fullName (required), employmentStatus (required), primarySpecializationId (required)
   - Optional: phone

2. **`TherapistWithAccountResponseDto`**
   - Contains both `UserCreationResponseDto` and `TherapistProfileAdminDto`

### New Service Method

**`TherapistProfileService.createTherapistWithAccount()`**

- Creates user with temporary password via `UserManagementService`
- Creates therapist profile with primary specialization
- Records audit entry
- Returns both user details (with temp password) and profile
- Transactional - both succeed or both fail

### Files Created

- `backend/src/main/java/com/psyassistant/therapists/dto/CreateTherapistWithAccountRequest.java`
- `backend/src/main/java/com/psyassistant/therapists/dto/TherapistWithAccountResponseDto.java`

### Files Modified

- `backend/src/main/java/com/psyassistant/therapists/rest/TherapistProfileController.java`  
  Added POST `/with-account` endpoint
  
- `backend/src/main/java/com/psyassistant/therapists/service/TherapistProfileService.java`  
  Added `createTherapistWithAccount()` method

## Frontend Changes

### Modified Components

1. **`CreateUserDialogComponent`**
   - Added new output event: `redirectToTherapistWizard`
   - Detects when THERAPIST role is selected in submit()
   - Emits prefilled data (email, fullName) instead of creating user
   - For non-therapist roles, proceeds with standard user creation

2. **`CreateTherapistDialogComponent`**
   - Added `@Input() prefilledData` to accept email and fullName
   - Updated `ngOnInit()` to apply prefilled data to form
   - Modified `submit()` to call new `/api/v1/therapists/with-account` endpoint
   - Extracts `userDetails` from response for credentials modal

3. **`UserListComponent`**
   - Added `showTherapistWizard` flag
   - Added `therapistPrefilledData` property
   - Added `onRedirectToTherapistWizard()` handler
   - Added `onTherapistCreated()` handler
   - Imported and registered `CreateTherapistDialogComponent`
   - Updated template to render therapist wizard with prefilled data

### Files Modified

- `frontend/src/app/features/admin/users/components/create-user-dialog/create-user-dialog.component.ts`
- `frontend/src/app/features/admin/therapists/components/create-therapist-dialog/create-therapist-dialog.component.ts`
- `frontend/src/app/features/admin/users/components/user-list/user-list.component.ts`

## Security

- Endpoint requires `SYSTEM_ADMINISTRATOR` or `ADMIN` role
- Temporary password is:
  - Auto-generated with secure random algorithm
  - Shown only once to admin
  - Never stored in plain text
  - Forces password change on first login
- Audit trail records creation with actor and timestamp

## Data Integrity

- **Atomic transaction:** Both user and profile creation succeed or both fail
- **Optimistic locking:** Profile updates use version column to prevent conflicts
- **Email uniqueness:** Validated at both user and profile level
- **Specialization validation:** Primary specialization must exist

## Testing

To test the flow:

1. Start backend: `cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=local`
2. Start frontend: `cd frontend && npm start`
3. Login as SYSTEM_ADMINISTRATOR
4. Navigate to User Management
5. Click "Create User"
6. Fill in email, full name
7. Select "THERAPIST" role
8. Click "Create User"
9. Verify wizard opens with prefilled email and name
10. Complete employment status and specialization
11. Submit
12. Verify temporary password modal appears
13. Copy password
14. Verify user list shows new therapist

## Future Enhancements

- [ ] Add languages selection to wizard
- [ ] Add multiple specializations in wizard
- [ ] Add bio/notes field in wizard
- [ ] Email temporary password to therapist automatically
- [ ] Support therapist self-registration with approval workflow
- [ ] Bulk therapist import from CSV

## Related Documentation

- [User Management Requirements](../../requirements.md#user-management)
- [Therapist Profile Management](./PA-22-lead-to-client-conversion.md)
- [Backend README](../../backend/README.md)
- [Frontend README](../../frontend/README.md)
