import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import {
  Payment,
  Refund,
  RegisterPaymentRequest,
  RegisterRefundRequest,
} from '../models/payment.model';

@Injectable({ providedIn: 'root' })
export class PaymentService {
  private readonly base = '/api/v1/invoices';

  constructor(private http: HttpClient) {}

  registerPayment(invoiceId: string, request: RegisterPaymentRequest): Observable<Payment> {
    return this.http.post<Payment>(`${this.base}/${invoiceId}/payments`, request);
  }

  getPayments(invoiceId: string): Observable<Payment[]> {
    return this.http.get<Payment[]>(`${this.base}/${invoiceId}/payments`);
  }

  registerRefund(invoiceId: string, request: RegisterRefundRequest): Observable<Refund> {
    return this.http.post<Refund>(`${this.base}/${invoiceId}/refunds`, request);
  }

  getRefunds(invoiceId: string): Observable<Refund[]> {
    return this.http.get<Refund[]>(`${this.base}/${invoiceId}/refunds`);
  }
}
