import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { CreateDiscountRuleRequest, DiscountRule } from '../models/discount.model';

@Injectable({ providedIn: 'root' })
export class DiscountService {
  private readonly base = '/api/v1/billing/discounts';

  constructor(private http: HttpClient) {}

  list(clientId?: string, serviceCatalogId?: string): Observable<DiscountRule[]> {
    let params = new HttpParams();
    if (clientId) {
      params = params.set('clientId', clientId);
    }
    if (serviceCatalogId) {
      params = params.set('serviceCatalogId', serviceCatalogId);
    }
    return this.http.get<DiscountRule[]>(this.base, { params });
  }

  create(request: CreateDiscountRuleRequest): Observable<DiscountRule> {
    return this.http.post<DiscountRule>(this.base, request);
  }

  toggleActive(id: string, active: boolean): Observable<DiscountRule> {
    return this.http.patch<DiscountRule>(`${this.base}/${id}/status`, { active });
  }
}
