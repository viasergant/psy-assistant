import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import {
  AddLineItemRequest,
  CancelInvoiceRequest,
  CreateInvoiceFromSessionRequest,
  CreateManualInvoiceRequest,
  Invoice,
  InvoiceListItem,
  InvoiceStatus,
  PagedResponse,
} from '../models/invoice.model';

export interface InvoiceListParams {
  clientId?: string;
  therapistId?: string;
  status?: InvoiceStatus;
  page?: number;
  size?: number;
}

@Injectable({ providedIn: 'root' })
export class InvoiceService {
  private readonly base = '/api/v1/invoices';

  constructor(private http: HttpClient) {}

  list(params: InvoiceListParams = {}): Observable<PagedResponse<InvoiceListItem>> {
    let httpParams = new HttpParams();
    if (params.clientId) { httpParams = httpParams.set('clientId', params.clientId); }
    if (params.therapistId) { httpParams = httpParams.set('therapistId', params.therapistId); }
    if (params.status) { httpParams = httpParams.set('status', params.status); }
    if (params.page !== undefined) { httpParams = httpParams.set('page', params.page); }
    if (params.size !== undefined) { httpParams = httpParams.set('size', params.size); }
    return this.http.get<PagedResponse<InvoiceListItem>>(this.base, { params: httpParams });
  }

  getById(id: string): Observable<Invoice> {
    return this.http.get<Invoice>(`${this.base}/${id}`);
  }

  createFromSession(request: CreateInvoiceFromSessionRequest): Observable<Invoice> {
    return this.http.post<Invoice>(`${this.base}/from-session`, request);
  }

  createManual(request: CreateManualInvoiceRequest): Observable<Invoice> {
    return this.http.post<Invoice>(`${this.base}/manual`, request);
  }

  addLineItem(invoiceId: string, request: AddLineItemRequest): Observable<Invoice> {
    return this.http.post<Invoice>(`${this.base}/${invoiceId}/line-items`, request);
  }

  removeLineItem(invoiceId: string, itemId: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/${invoiceId}/line-items/${itemId}`);
  }

  issue(invoiceId: string): Observable<Invoice> {
    return this.http.post<Invoice>(`${this.base}/${invoiceId}/issue`, {});
  }

  cancel(invoiceId: string, request: CancelInvoiceRequest): Observable<Invoice> {
    return this.http.post<Invoice>(`${this.base}/${invoiceId}/cancel`, request);
  }

  downloadPdf(invoiceId: string): Observable<Blob> {
    return this.http.get(`${this.base}/${invoiceId}/pdf`, { responseType: 'blob' });
  }
}
