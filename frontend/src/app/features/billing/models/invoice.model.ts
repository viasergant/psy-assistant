export type InvoiceStatus = 'DRAFT' | 'ISSUED' | 'OVERDUE' | 'PAID' | 'CANCELLED';
export type InvoiceSource = 'SESSION' | 'PACKAGE' | 'MANUAL';

export interface InvoiceLineItem {
  id: string;
  description: string;
  quantity: number;
  unitPrice: number;
  lineTotal: number;
  sortOrder: number;
}

export interface Invoice {
  id: string;
  invoiceNumber: string;
  clientId: string;
  therapistId: string | null;
  source: InvoiceSource;
  sessionId: string | null;
  prepaidPackageId: string | null;
  status: InvoiceStatus;
  issuedDate: string | null;
  dueDate: string | null;
  cancellationReason: string | null;
  cancelledAt: string | null;
  subtotal: number;
  discount: number;
  total: number;
  notes: string | null;
  pdfPath: string | null;
  lineItems: InvoiceLineItem[];
  createdAt: string;
  updatedAt: string;
}

export interface InvoiceListItem {
  id: string;
  invoiceNumber: string;
  clientId: string;
  therapistId: string | null;
  source: InvoiceSource;
  status: InvoiceStatus;
  issuedDate: string | null;
  dueDate: string | null;
  total: number;
  createdAt: string;
}

export interface PagedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export interface CreateInvoiceFromSessionRequest {
  sessionId: string;
  unitPriceOverride: number;
  notes?: string;
}

export interface LineItemRequest {
  description: string;
  quantity: number;
  unitPrice: number;
}

export interface CreateManualInvoiceRequest {
  clientId: string;
  therapistId?: string;
  lineItems: LineItemRequest[];
  discount?: number;
  notes?: string;
}

export interface AddLineItemRequest {
  description: string;
  quantity: number;
  unitPrice: number;
}

export interface CancelInvoiceRequest {
  reason: string;
}
