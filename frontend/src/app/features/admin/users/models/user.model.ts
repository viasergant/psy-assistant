/** User roles supported by the system (current scoped values + legacy aliases). */
export type UserRole =
  | 'RECEPTION_ADMIN_STAFF'
  | 'THERAPIST'
  | 'SUPERVISOR'
  | 'FINANCE'
  | 'SYSTEM_ADMINISTRATOR'
  | 'ADMIN'   // legacy — kept for backward-compat parsing
  | 'USER';   // legacy — kept for backward-compat parsing

/** Human-readable labels for every role value the backend may return. */
export const ROLE_LABELS: Record<string, string> = {
  RECEPTION_ADMIN_STAFF: 'Reception / Admin Staff',
  THERAPIST: 'Therapist',
  SUPERVISOR: 'Supervisor',
  FINANCE: 'Finance',
  SYSTEM_ADMINISTRATOR: 'System Administrator',
  ADMIN: 'System Administrator',  // legacy alias
  USER: 'Therapist',              // legacy alias
};

/** Canonical roles available for selection (create / edit / filter). */
export const ASSIGNABLE_ROLES: UserRole[] = [
  'RECEPTION_ADMIN_STAFF',
  'THERAPIST',
  'SUPERVISOR',
  'FINANCE',
  'SYSTEM_ADMINISTRATOR',
];

/** Normalise legacy role values that may come from old tokens or un-migrated data. */
export function normalizeRole(role: string): UserRole {
  if (role === 'ADMIN') { return 'SYSTEM_ADMINISTRATOR'; }
  if (role === 'USER')  { return 'THERAPIST'; }
  return role as UserRole;
}

/** Read-only summary of a user account. */
export interface UserSummary {
  id: string;
  email: string;
  fullName: string | null;
  role: UserRole;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

/**
 * Response returned after creating a new user account.
 * Contains the auto-generated temporary password that must be shown to the admin.
 */
export interface UserCreationResponse {
  id: string;
  email: string;
  fullName: string | null;
  role: UserRole;
  temporaryPassword: string;
}

/** Paginated list response. */
export interface UserPage {
  content: UserSummary[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
}

/** Query parameters for the user list endpoint. */
export interface UserListParams {
  page: number;
  size: number;
  sort: string;
  role?: UserRole;
  active?: boolean;
}

/** Request body for creating a new user. */
export interface CreateUserPayload {
  email: string;
  fullName: string;
  role: UserRole;
}

/** Request body for a partial user update. */
export interface PatchUserPayload {
  fullName?: string;
  role?: UserRole;
  active?: boolean;
}
