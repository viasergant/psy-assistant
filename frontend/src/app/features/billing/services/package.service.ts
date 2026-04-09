import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import {
  CreatePackageDefinitionRequest,
  PackageDefinition,
  PackageDefinitionStatus,
  PackageInstance,
  SellPackageRequest,
  UpdatePackageDefinitionStatusRequest,
} from '../models/package.model';

@Injectable({ providedIn: 'root' })
export class PackageService {
  private readonly defsBase = '/api/v1/billing/package-definitions';
  private readonly sellBase = '/api/v1/billing/packages/sell';

  constructor(private http: HttpClient) {}

  listDefinitions(status?: PackageDefinitionStatus): Observable<PackageDefinition[]> {
    let params = new HttpParams();
    if (status) {
      params = params.set('status', status);
    }
    return this.http.get<PackageDefinition[]>(this.defsBase, { params });
  }

  createDefinition(request: CreatePackageDefinitionRequest): Observable<PackageDefinition> {
    return this.http.post<PackageDefinition>(this.defsBase, request);
  }

  updateDefinitionStatus(
    id: string,
    request: UpdatePackageDefinitionStatusRequest,
  ): Observable<PackageDefinition> {
    return this.http.patch<PackageDefinition>(`${this.defsBase}/${id}/status`, request);
  }

  listClientPackages(clientId: string): Observable<PackageInstance[]> {
    return this.http.get<PackageInstance[]>(`/api/v1/clients/${clientId}/packages`);
  }

  sellPackage(request: SellPackageRequest): Observable<PackageInstance> {
    return this.http.post<PackageInstance>(this.sellBase, request);
  }
}
