import { UserRole } from '../../../admin/users/models/user.model';

/** Organization kind, mirrors the backend OrganizationType enum. */
export type OrganizationType = 'SOLO' | 'CLINIC';

/** Read-only user record visible to the global administrator. */
export interface GlobalUser {
  id: string;
  email: string;
  fullName: string | null;
  roles: UserRole[];
  active: boolean;
  createdAt: string;
  orgId: string | null;
  orgName: string | null;
  orgType: OrganizationType | null;
}

/** Paginated response of GET /api/v1/global-admin/users. */
export interface GlobalUserPage {
  content: GlobalUser[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
}

/** Query parameters accepted by the global user list endpoint. */
export interface GlobalUserListParams {
  page: number;
  size: number;
  sort: string;
  role?: UserRole;
  active?: boolean;
  search?: string;
}
