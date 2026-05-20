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

/**
 * Normalise an array of raw role strings from the API (may include legacy aliases).
 * Falls back to `['THERAPIST']` when the input is empty or undefined.
 */
export function normalizeRoles(roles: string[] | undefined): UserRole[] {
  if (!roles || roles.length === 0) { return []; }
  return roles.map(normalizeRole);
}

/** Read-only summary of a user account. */
export interface UserSummary {
  id: string;
  email: string;
  fullName: string | null;
  /**
   * @deprecated Use `roles` instead. Retained for backward-compat with
   * API responses that still include a single `role` field.
   */
  role: UserRole;
  /** All roles assigned to this user. Use `roles ?? [role]` to handle both old and new shapes. */
  roles?: UserRole[];
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
  /**
   * @deprecated Use `roles` instead.
   */
  role: UserRole;
  /** All roles assigned to the created user. */
  roles?: UserRole[];
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
  /** One or more roles to assign. At least one role is required. */
  roles: UserRole[];
}

/** Request body for a partial user update. */
export interface PatchUserPayload {
  fullName?: string;
  /** Replaces the user's role set. Must have at least one element when provided. */
  roles?: UserRole[];
  active?: boolean;
}
