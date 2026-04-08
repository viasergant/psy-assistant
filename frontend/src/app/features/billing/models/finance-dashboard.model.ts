export interface AgingBuckets {
  current030: number;
  past3160: number;
  past60plus: number;
}

export interface FinanceDashboard {
  totalOutstandingAmount: number;
  totalOverdueAmount: number;
  collectedThisMonthAmount: number;
  aging: AgingBuckets;
  asOf: string;
}
