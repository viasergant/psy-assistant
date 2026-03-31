# Leave Request Approval Feature Implementation Summary

## Overview
Implemented the ability for admins (SYSTEM_ADMINISTRATOR) and reception staff (RECEPTION_ADMIN_STAFF) to view pending vacation/leave requests and approve or reject them.

## Backend Changes

### 1. New Admin Controller
**File**: `backend/src/main/java/com/psyassistant/scheduling/rest/LeaveAdminController.java`

Created a dedicated REST controller for administrative leave operations:
- **GET `/api/v1/admin/leave/pending`** - Retrieves all pending leave requests across all therapists
- **PUT `/api/v1/admin/leave/{leaveId}/approve`** - Approves a leave request
- **PUT `/api/v1/admin/leave/{leaveId}/reject`** - Rejects a leave request

All endpoints are secured with `@PreAuthorize` to allow both `SYSTEM_ADMINISTRATOR` and `RECEPTION_ADMIN_STAFF` roles.

### 2. Updated Permissions
**File**: `backend/src/main/java/com/psyassistant/scheduling/rest/TherapistLeaveController.java`

Updated existing approve/reject endpoints to also allow `RECEPTION_ADMIN_STAFF` role:
- Changed from `@PreAuthorize("hasRole('SYSTEM_ADMINISTRATOR')")` 
- To `@PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR', 'RECEPTION_ADMIN_STAFF')")`

Added deprecation notices pointing to the new admin endpoints for better API design.

## Frontend Changes

### 1. Updated Service
**File**: `frontend/src/app/features/schedule/services/leave-request.service.ts`

Added three new methods for admin operations:
- `getAllPendingLeaveRequests()` - Fetches all pending requests
- `approveLeaveRequestAdmin(leaveId, request)` - Approves a request
- `rejectLeaveRequestAdmin(leaveId, request)` - Rejects a request

### 2. New Admin Layout Component
**File**: `frontend/src/app/features/admin/admin-layout.component.ts`

Created a tabbed layout component for the admin area with navigation tabs:
- Users
- Therapists
- Pending Requests (new)

Features:
- Clean tabbed navigation with active state highlighting
- Router outlet for child components
- Accessible ARIA attributes for screen readers

### 3. New Admin Component
**Files**:
- `frontend/src/app/features/admin/leave/components/pending-leave-requests/pending-leave-requests.component.ts`
- `frontend/src/app/features/admin/leave/components/pending-leave-requests/pending-leave-requests.component.html`
- `frontend/src/app/features/admin/leave/components/pending-leave-requests/pending-leave-requests.component.scss`

Created a new component with a split-pane layout:
- **Left pane**: List of all pending leave requests with:
  - Therapist name (when available)
  - Leave type badge
  - Date range
  - Submission timestamp
  - Preview of therapist notes
- **Right pane**: Detailed review interface with:
  - Full request details
  - Admin notes input field
  - Approve/Reject/Cancel buttons

Features:
- Real-time loading states
- Empty state when no pending requests
- Visual feedback for selected requests
- Required notes for rejection (optional for approval)
- Automatic current user ID extraction from JWT token using `jwtDecode`

### 4. Updated Routing
**File**: `frontend/src/app/features/admin/admin.routes.ts`

- Added `AdminLayoutComponent` as parent with child routes
- Added route for `/admin/leave-requests` pointing to `PendingLeaveRequestsComponent`
- Restructured to use nested routing for better organization

### 5. Updated Models
**File**: `frontend/src/app/features/schedule/models/schedule.model.ts`

- Updated `Leave` interface to include optional `therapistName` field
- Fixed `LeaveApprovalRequest` interface to match backend requirements:
  - `reviewerUserId: string` (required)
  - `adminNotes?: string` (optional)

### 6. Updated Translations
**File**: `frontend/src/assets/i18n/en.json`

Added new translation keys:
- `admin.title` - "Administration"
- `admin.tabs.users` - "Users"
- `admin.tabs.therapists` - "Therapists"
- `admin.tabs.leaveRequests` - "Pending Requests"
- `admin.leave.pendingRequests`
- `admin.leave.noPendingRequests`
- `admin.leave.reviewRequest`
- `admin.leave.therapist`
- `admin.leave.submittedAt`
- `admin.leave.therapistNotes`
- `admin.leave.adminNotes`
- `admin.leave.selectRequestToReview`
- `common.cancel`

## API Endpoints

### New Admin Endpoints
```
GET    /api/v1/admin/leave/pending              → Get all pending requests
PUT    /api/v1/admin/leave/{leaveId}/approve    → Approve a request
PUT    /api/v1/admin/leave/{leaveId}/reject     → Reject a request
```

### Request Body for Approve/Reject
```json
{
  "reviewerUserId": "uuid-of-admin-or-staff",
  "adminNotes": "Optional notes explaining the decision"
}
```

## Security & Authorization

Both `SYSTEM_ADMINISTRATOR` and `RECEPTION_ADMIN_STAFF` roles can:
- ✅ View all pending leave requests
- ✅ Approve leave requests
- ✅ Reject leave requests

## Usage Instructions

The component is now fully integrated into the admin area with tabbed navigation. Users can access it by:

1. Navigate to `/admin` (redirects to `/admin/users` by default)
2. Click on the "Pending Requests" tab in the admin navigation
3. View and approve/reject leave requests

The admin layout automatically:
- Shows active tab highlighting
- Preserves navigation state
- Handles routing to child components

Authentication:
- Current user ID is automatically extracted from the JWT token
- No manual configuration needed

## Future Enhancements
1. Create DTO with therapist name joined from therapist_profile table
2. Add pagination for large numbers of pending requests
3. Add filters (by leave type, date range, therapist)
4. Add bulk approval capabilities
5. Add email notifications when requests are approved/rejected
6. Add real-time updates using WebSocket or SSE

## Testing Checklist
- [ ] Admin can view all pending requests
- [ ] Reception staff can view all pending requests
- [ ] Admin can approve requests
- [ ] Reception staff can approve requests
- [ ] Admin can reject requests (with mandatory notes)
- [ ] Reception staff can reject requests (with mandatory notes)
- [ ] Therapists cannot access admin endpoints
- [ ] Pending list refreshes after approval/rejection
- [ ] Proper error handling for failed requests

## Files Created/Modified

### Backend
- ✨ Created: `LeaveAdminController.java`
- 📝 Modified: `TherapistLeaveController.java` (updated permissions and documentation)

### Frontend
- ✨ Created: `pending-leave-requests.component.ts`
- ✨ Created: `admin-layout.component.ts` (tabbed navigation)
- ✨ Created: `pending-leave-requests.component.ts`
- ✨ Created: `pending-leave-requests.component.html`
- ✨ Created: `pending-leave-requests.component.scss`
- 📝 Modified: `admin.routes.ts` (added layout wrapper and leave-requests route)in methods)
- 📝 Modified: `schedule.model.ts` (updated interfaces)
- 📝 Modified: `en.json` (added translations)
