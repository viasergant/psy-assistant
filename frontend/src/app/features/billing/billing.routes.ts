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
  },
  {
    path: 'catalog',
    loadComponent: () =>
      import('./components/service-catalog-list/service-catalog-list.component')
        .then(m => m.ServiceCatalogListComponent)
  },
  {
    path: 'catalog/:id',
    loadComponent: () =>
      import('./components/service-catalog-detail/service-catalog-detail.component')
        .then(m => m.ServiceCatalogDetailComponent)
  }
] satisfies Routes;

