/** User roles supported by the system. */
export type UserRole = 'ADMIN' | 'USER';

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
