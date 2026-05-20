/**
 * Admin-facing risk flag type model.
 * Mirrors RiskFlagType from the clients/risk-flags feature (PA-27).
 */
export interface RiskFlagType {
  id: string;
  name: string;
  displayOrder: number;
  active: boolean;
}
