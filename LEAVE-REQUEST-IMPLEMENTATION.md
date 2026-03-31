# Leave Request Button Implementation

## Summary

Implemented the "Request Leave" button functionality in the Schedule Management page. The feature allows therapists to submit leave requests through a modal dialog, which are then sent to the backend for approval workflow.

## Changes Made

### 1. Created Leave Request Dialog Component
**File**: `frontend/src/app/features/schedule/components/leave-request-dialog/leave-request-dialog.component.ts`

- Standalone Angular component with reactive forms
- Fields:
  - Leave Type (dropdown): Annual, Sick, Public Holiday, Other
  - Start Date (date picker)
  - End Date (date picker with validation)
  - Request Notes (optional textarea)
- Form validation:
  - All required fields validated
  - End date must be >= start date
  - Real-time error messages
- Emits events:
  - `submitted`: When leave request is successfully created
  - `cancelled`: When user closes the modal
- Follows existing dialog pattern in the codebase (custom overlay, no PrimeNG p-dialog)

### 2. Updated Schedule Management Component
**File**: `frontend/src/app/features/schedule/schedule-management.component.ts`

- Imported `LeaveRequestDialogComponent` and `Leave` model
- Added `showLeaveRequestModal` state flag
- Implemented modal lifecycle methods:
  - `openLeaveRequestModal()`: Opens the dialog
  - `closeLeaveRequestModal()`: Closes the dialog
  - `onLeaveRequestSubmitted(leave)`: Handles successful submission, reloads schedule, shows success alert
- Added dialog to component template with proper bindings

### 3. Backend Integration
The implementation uses the existing backend API:
- **Endpoint**: `POST /api/v1/therapists/{therapistProfileId}/leave`
- **Service**: `LeaveRequestService.submitLeaveRequest()`
- **Request DTO**: Matches `LeaveRequestSubmission` record with fields:
  - `startDate`: LocalDate
  - `endDate`: LocalDate
  - `leaveType`: LeaveType enum
  - `requestNotes`: String (optional)
- **Response**: Returns the created `TherapistLeave` entity with PENDING status

## User Flow

1. User clicks "Request Leave" button in Schedule Management header
2. Modal dialog opens with form
3. User fills in:
   - Leave type (required)
   - Start date (defaults to today, required)
   - End date (defaults to today, required, must be >= start date)
   - Optional notes
4. User clicks "Submit Request"
5. Frontend sends POST request to backend
6. Backend creates leave request with PENDING status
7. Modal closes and shows success alert
8. Schedule reloads to reflect the new leave period

## Technical Details

### Form Validation
- Custom validator ensures end date >= start date
- Real-time validation feedback
- Submit button disabled when form invalid or saving

### Error Handling
- HTTP errors displayed in modal
- Server error messages shown to user
- Forms remain open on error for correction

### Styling
- Consistent with existing dialog components
- Uses CSS custom properties for theming
- Responsive design (max 95vw width)
- Accessible (ARIA attributes, focus management)

## Testing Recommendations

1. **Functional Testing**:
   - Submit leave request with all leave types
   - Try invalid date ranges (end before start)
   - Test with and without optional notes
   - Verify backend creates PENDING status

2. **UI Testing**:
   - Verify modal opens/closes correctly
   - Check form validation messages
   - Test responsive behavior on mobile
   - Verify accessibility with screen reader

3. **Integration Testing**:
   - Confirm schedule reloads after submission
   - Verify leave appears in calendar view
   - Test with different therapist profiles (admin view)

4. **Error Scenarios**:
   - Network errors
   - Backend validation errors
   - Duplicate date ranges
   - Past dates (backend should reject)

## Future Enhancements

1. **Conflict Detection**: Show warning if leave overlaps with scheduled appointments
2. **Toast Notifications**: Replace alert() with toast/snackbar component
3. **Date Range Validation**: Call backend conflict check endpoint before submission
4. **Inline Success**: Show success message in UI instead of alert
5. **Leave List View**: Add view to see all submitted leave requests with status
6. **Cancel Request**: Add ability to cancel pending leave requests

## Related Files

### Frontend
- `frontend/src/app/features/schedule/components/leave-request-dialog/leave-request-dialog.component.ts`
- `frontend/src/app/features/schedule/schedule-management.component.ts`
- `frontend/src/app/features/schedule/services/leave-request.service.ts`
- `frontend/src/app/features/schedule/models/schedule.model.ts`

### Backend (Already Implemented)
- `backend/src/main/java/com/psyassistant/scheduling/rest/TherapistLeaveController.java`
- `backend/src/main/java/com/psyassistant/scheduling/service/TherapistLeaveService.java`
- `backend/src/main/java/com/psyassistant/scheduling/domain/TherapistLeave.java`
- `backend/src/main/java/com/psyassistant/scheduling/dto/LeaveRequestSubmission.java`
- `backend/src/main/resources/db/migration/V21__therapist_leave.sql`

## Build Status

✅ Frontend build successful (no TypeScript errors)
✅ Component imports correct
✅ Service integration verified
✅ Backend API contract matches frontend
