import { Routes } from '@angular/router';

export default [
  {
    path: '',
    redirectTo: 'invoices',
    pathMatch: 'full'
  },
  {
    path: 'dashboard',
    loadComponent: () =>
      import('./components/finance-dashboard/finance-dashboard.component')
        .then(m => m.FinanceDashboardComponent)
  },
  {
    path: 'invoices',
    loadComponent: () =>
      import('./components/invoice-list/invoice-list.component')
        .then(m => m.InvoiceListComponent)
  },
  {
    path: 'invoices/new',
    loadComponent: () =>
      import('./components/invoice-form/invoice-form.component')
        .then(m => m.InvoiceFormComponent)
  },
  {
    path: 'invoices/:id',
    loadComponent: () =>
      import('./components/invoice-detail/invoice-detail.component')
        .then(m => m.InvoiceDetailComponent)
  }
] satisfies Routes;

