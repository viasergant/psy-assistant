export type ServiceStatus = 'ACTIVE' | 'INACTIVE';

export interface SessionTypeInfo {
  id: string;
  code: string;
  name: string;
  description?: string;
}

export interface ServiceCatalogItem {
  id: string;
  name: string;
  category: string;
  sessionType: SessionTypeInfo;
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
  sessionTypeId: string;
  durationMin: number;
  defaultPrice: number;
  effectiveFrom: string;
}

export interface UpdateServiceRequest {
  name: string;
  category: string;
  sessionTypeId: string;
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
