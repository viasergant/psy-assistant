/**
 * Therapist profile models mirroring backend DTOs.
 */

export interface IdNamePair {
  id: string;
  name: string;
}

/** Profile completion status for therapist onboarding workflow. */
export type ProfileCompletionStatus = 'INCOMPLETE' | 'COMPLETE';

export const PROFILE_COMPLETION_LABELS: Record<ProfileCompletionStatus, string> = {
  INCOMPLETE: 'Incomplete',
  COMPLETE: 'Complete'
};

export interface TherapistProfile {
  id: string;
  email: string;
  name: string;
  phone?: string;
  employmentStatus: EmploymentStatus;
  bio?: string;
  active: boolean;
  version: number;
  profileCompletionStatus: ProfileCompletionStatus;
  specializations: IdNamePair[];
  languages: IdNamePair[];
  createdAt: string;
  createdBy: string;
  lastModifiedAt?: string;
  lastModifiedBy?: string;
}

export interface CreateTherapistRequest {
  email: string;
  name: string;
  phone?: string;
  employmentStatus?: EmploymentStatus;
  bio?: string;
  specializationIds: string[];
  languageIds: string[];
}

export interface UpdateTherapistRequest {
  email?: string;
  name?: string;
  phone?: string;
  employmentStatus?: EmploymentStatus;
  bio?: string;
  version: number;
}

export type EmploymentStatus = 'ACTIVE' | 'ON_LEAVE' | 'INACTIVE';

export const EMPLOYMENT_STATUS_OPTIONS: EmploymentStatus[] = [
  'ACTIVE',
  'ON_LEAVE',
  'INACTIVE'
];

export const EMPLOYMENT_STATUS_LABELS: Record<EmploymentStatus, string> = {
  ACTIVE: 'Active',
  ON_LEAVE: 'On Leave',
  INACTIVE: 'Inactive'
};

export interface TherapistPage {
  content: TherapistProfile[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
