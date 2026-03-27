/** Lead status values in lifecycle order. */
export type LeadStatus = 'NEW' | 'CONTACTED' | 'QUALIFIED' | 'CONVERTED' | 'INACTIVE';

/** Contact method types supported by the system. */
export type ContactMethodType = 'EMAIL' | 'PHONE';

/** Human-readable labels for lead statuses. */
export const LEAD_STATUS_LABELS: Record<LeadStatus, string> = {
  NEW: 'New',
  CONTACTED: 'Contacted',
  QUALIFIED: 'Qualified',
  CONVERTED: 'Converted',
  INACTIVE: 'Archived',
};

/** Statuses that allow further transitions (non-terminal). */
export const ACTIVE_STATUSES: LeadStatus[] = ['NEW', 'CONTACTED', 'QUALIFIED'];

/** Statuses selectable in the list filter (all values). */
export const ALL_STATUSES: LeadStatus[] = ['NEW', 'CONTACTED', 'QUALIFIED', 'CONVERTED', 'INACTIVE'];

/** Contact method on an existing lead. */
export interface ContactMethod {
  id: string;
  type: ContactMethodType;
  value: string;
  isPrimary: boolean;
}

/** Contact method in create/update requests. */
export interface ContactMethodRequest {
  type: ContactMethodType;
  value: string;
  isPrimary: boolean;
}

/** Summary row returned in paginated list. */
export interface LeadSummary {
  id: string;
  fullName: string;
  primaryContact: string | null;
  source: string | null;
  status: LeadStatus;
  ownerName: string | null;
  lastContactDate: string | null;
  createdAt: string;
}

/** Full detail returned for a single lead or after a mutation. */
export interface LeadDetail {
  id: string;
  fullName: string;
  contactMethods: ContactMethod[];
  source: string | null;
  status: LeadStatus;
  ownerId: string | null;
  ownerName: string | null;
  notes: string | null;
  lastContactDate: string | null;
  createdAt: string;
  updatedAt: string;
  createdBy: string;
  /** UUID of the client record this lead was converted to, or null. */
  convertedClientId: string | null;
}

/** Paginated list response. */
export interface LeadPage {
  content: LeadSummary[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
}

/** Query parameters for the lead list endpoint. */
export interface LeadListParams {
  page: number;
  size: number;
  sort: string;
  status?: LeadStatus;
  ownerId?: string;
  includeArchived?: boolean;
}

/** Request body for creating a new lead. */
export interface CreateLeadPayload {
  fullName: string;
  contactMethods: ContactMethodRequest[];
  source?: string;
  ownerId?: string;
  notes?: string;
}

/** Request body for updating a lead. */
export interface UpdateLeadPayload {
  fullName: string;
  contactMethods: ContactMethodRequest[];
  source?: string;
  ownerId?: string;
  notes?: string;
}

/** Request body for status transitions. */
export interface TransitionStatusPayload {
  status: LeadStatus;
}

/** Request body for converting a qualified lead to a client. */
export interface ConvertLeadPayload {
  fullName: string;
  contactMethods: ContactMethodRequest[];
  notes?: string;
  ownerId?: string;
}

/** Response body returned after a successful lead-to-client conversion. */
export interface ConvertLeadResponse {
  clientId: string;
  leadId: string;
}

/** Error body returned for 409 LEAD_ALREADY_CONVERTED responses. */
export interface ConversionErrorBody {
  code: string;
  existingClientId: string | null;
}
