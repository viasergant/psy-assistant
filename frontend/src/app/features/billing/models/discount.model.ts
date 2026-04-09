export type DiscountType = 'PERCENTAGE' | 'FIXED_AMOUNT';
export type DiscountScope = 'CLIENT' | 'SERVICE';

export interface DiscountRule {
  id: string;
  name: string;
  type: DiscountType;
  value: number;
  scope: DiscountScope;
  clientId: string | null;
  serviceCatalogId: string | null;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CreateDiscountRuleRequest {
  name: string;
  type: DiscountType;
  value: number;
  scope: DiscountScope;
  clientId?: string;
  serviceCatalogId?: string;
}
