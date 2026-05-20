export type RiskFlagStatus = 'ACTIVE' | 'RESOLVED';

export interface RiskFlagType {
  id: string;
  name: string;
  displayOrder: number;
  active: boolean;
}

export interface RiskFlag {
  id: string;
  clientId: string;
  flagTypeId: string;
  flagTypeName: string;
  status: RiskFlagStatus;
  /** null when caller lacks READ_RISK_FLAG_NOTES permission */
  clinicalNote: string | null;
  /** ISO date string (yyyy-MM-dd) */
  reviewDate: string;
  createdByUserId: string;
  createdAt: string;
  resolvedByUserId: string | null;
  resolvedAt: string | null;
  resolutionNote: string | null;
}

export interface CreateRiskFlagPayload {
  flagTypeId: string;
  clinicalNote: string | null;
  /** ISO date string (yyyy-MM-dd) */
  reviewDate: string;
}

export interface ResolveRiskFlagPayload {
  resolutionNote: string;
}
