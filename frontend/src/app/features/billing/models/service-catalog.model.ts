export type ServiceStatus = 'ACTIVE' | 'INACTIVE';

export type ServiceType =
  | 'INDIVIDUAL_SESSION'
  | 'GROUP_SESSION'
  | 'INTAKE_ASSESSMENT'
  | 'FOLLOW_UP';

export interface ServiceCatalogItem {
  id: string;
  name: string;
  category: string;
  serviceType: ServiceType;
  durationMin: number;
  status: ServiceStatus;
  currentPrice: number;
  createdAt: string;
  updatedAt: string;
}

export interface PriceHistoryEntry {
  id: string;
  price: number;
  effectiveFrom: string;
  effectiveTo: string | null;
  changedBy: string;
}

export interface TherapistOverride {
  id: string;
  therapistId: string;
  therapistName: string;
  price: number;
}

export interface CreateServiceRequest {
  name: string;
  category: string;
  serviceType: ServiceType;
  durationMin: number;
  defaultPrice: number;
  effectiveFrom: string;
}

export interface UpdateServiceRequest {
  name: string;
  category: string;
  serviceType: ServiceType;
  durationMin: number;
}

export interface UpdateDefaultPriceRequest {
  price: number;
  effectiveFrom: string;
}

export interface UpdateStatusRequest {
  status: ServiceStatus;
}

export interface UpsertTherapistOverrideRequest {
  price: number;
}
