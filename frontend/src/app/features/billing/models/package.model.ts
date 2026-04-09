export type PackageDefinitionStatus = 'ACTIVE' | 'ARCHIVED';
export type PackageInstanceStatus = 'ACTIVE' | 'EXHAUSTED' | 'EXPIRED';

export type ServiceType =
  | 'INDIVIDUAL_SESSION'
  | 'GROUP_SESSION'
  | 'INTAKE_ASSESSMENT'
  | 'FOLLOW_UP';

export interface PackageDefinition {
  id: string;
  name: string;
  serviceType: ServiceType;
  sessionQty: number;
  price: number;
  perSessionDisplay: number;
  status: PackageDefinitionStatus;
  createdAt: string;
  updatedAt: string;
}

export interface PackageInstance {
  id: string;
  definitionId: string;
  definitionName: string;
  serviceType: ServiceType;
  packagePrice: number;
  clientId: string;
  purchasedAt: string;
  invoiceId: string | null;
  sessionsRemaining: number;
  sessionsTotal: number;
  status: PackageInstanceStatus;
  expiresAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreatePackageDefinitionRequest {
  name: string;
  serviceType: ServiceType;
  sessionQty: number;
  price: number;
}

export interface UpdatePackageDefinitionStatusRequest {
  status: PackageDefinitionStatus;
}

export interface SellPackageRequest {
  definitionId: string;
  clientId: string;
  purchasedAt: string;
  validityDays: number | null;
}
