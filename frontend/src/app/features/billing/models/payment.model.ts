import { InvoiceStatus } from './invoice.model';

export type PaymentMethod = 'CASH' | 'BANK_TRANSFER' | 'CARD';

export interface Payment {
  id: string;
  invoiceId: string;
  amount: number;
  paymentMethod: PaymentMethod;
  paymentDate: string;
  reference: string | null;
  notes: string | null;
  createdBy: string;
  createdAt: string;
  invoiceOutstandingBalance: number;
  invoiceStatus: InvoiceStatus;
}

export interface Refund {
  id: string;
  invoiceId: string;
  paymentId: string | null;
  amount: number;
  reason: string;
  refundDate: string;
  reference: string | null;
  createdBy: string;
  createdAt: string;
}

export interface RegisterPaymentRequest {
  amount: number;
  paymentMethod: PaymentMethod;
  paymentDate: string;
  reference?: string;
  notes?: string;
}

export interface RegisterRefundRequest {
  amount: number;
  reason: string;
  refundDate: string;
  paymentId?: string;
  reference?: string;
}

export interface WalletResponse {
  clientId: string;
  balance: number;
}
