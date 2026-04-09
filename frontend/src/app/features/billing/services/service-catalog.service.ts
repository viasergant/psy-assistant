import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import {
  ServiceCatalogItem,
  ServiceStatus,
  PriceHistoryEntry,
  TherapistOverride,
  CreateServiceRequest,
  UpdateServiceRequest,
  UpdateDefaultPriceRequest,
  UpdateStatusRequest,
  UpsertTherapistOverrideRequest,
} from '../models/service-catalog.model';

@Injectable({ providedIn: 'root' })
export class ServiceCatalogService {
  private readonly base = '/api/v1/billing/catalog';

  constructor(private http: HttpClient) {}

  list(status?: ServiceStatus): Observable<ServiceCatalogItem[]> {
    let params = new HttpParams();
    if (status) {
      params = params.set('status', status);
    }
    return this.http.get<ServiceCatalogItem[]>(this.base, { params });
  }

  getById(id: string): Observable<ServiceCatalogItem> {
    return this.http.get<ServiceCatalogItem>(`${this.base}/${id}`);
  }

  create(request: CreateServiceRequest): Observable<ServiceCatalogItem> {
    return this.http.post<ServiceCatalogItem>(this.base, request);
  }

  update(id: string, request: UpdateServiceRequest): Observable<ServiceCatalogItem> {
    return this.http.patch<ServiceCatalogItem>(`${this.base}/${id}`, request);
  }

  updateStatus(id: string, request: UpdateStatusRequest): Observable<ServiceCatalogItem> {
    return this.http.patch<ServiceCatalogItem>(`${this.base}/${id}/status`, request);
  }

  updatePrice(id: string, request: UpdateDefaultPriceRequest): Observable<ServiceCatalogItem> {
    return this.http.patch<ServiceCatalogItem>(`${this.base}/${id}/price`, request);
  }

  getPriceHistory(id: string): Observable<PriceHistoryEntry[]> {
    return this.http.get<PriceHistoryEntry[]>(`${this.base}/${id}/price-history`);
  }

  getOverrides(id: string): Observable<TherapistOverride[]> {
    return this.http.get<TherapistOverride[]>(`${this.base}/${id}/overrides`);
  }

  upsertOverride(
    serviceId: string,
    therapistId: string,
    request: UpsertTherapistOverrideRequest,
  ): Observable<TherapistOverride> {
    return this.http.put<TherapistOverride>(
      `${this.base}/${serviceId}/overrides/${therapistId}`,
      request,
    );
  }

  removeOverride(serviceId: string, therapistId: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/${serviceId}/overrides/${therapistId}`);
  }
}
